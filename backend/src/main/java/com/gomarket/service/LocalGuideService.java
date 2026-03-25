package com.gomarket.service;

import com.gomarket.dto.LocalGuideResponse;
import com.gomarket.dto.LocalGuideResponse.SuggestionItem;
import com.gomarket.dto.RecipeResponse.WeatherInfo;
import com.gomarket.model.Post;
import com.gomarket.model.User;
import com.gomarket.repository.PostRepository;
import com.gomarket.repository.UserRepository;
import com.google.gson.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AI Thổ Địa — Gợi ý đặc sản theo mùa, vị trí, và khẩu vị người dùng
 *
 * Luồng:
 * 1. Thu thập context: location (city), month (seasonality), taste_profile
 * 2. Gọi Gemini với prompt kết hợp 3 yếu tố → 3 gợi ý đặc sản
 * 3. RAG match: vector search bài đăng chợ đồng hương cho mỗi gợi ý
 * 4. Nếu 0 kết quả → trả về chế độ "kích cầu" (UI hiện nút đăng tin)
 */
@Service
public class LocalGuideService {

    private static final Logger log = LoggerFactory.getLogger(LocalGuideService.class);

    private final WeatherService weatherService;
    private final EmbeddingService embeddingService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    @Value("${api.openrouter.key}")
    private String apiKey;

    @Value("${api.openrouter.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    @Value("${api.openrouter.model:google/gemini-2.0-flash-001}")
    private String modelName;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    // Cache gợi ý 24h (key: userId_date)
    private final Map<String, LocalGuideResponse> suggestionCache = new ConcurrentHashMap<>();

    public LocalGuideService(WeatherService weatherService, EmbeddingService embeddingService,
                             PostRepository postRepository, UserRepository userRepository,
                             GeminiService geminiService) {
        this.weatherService = weatherService;
        this.embeddingService = embeddingService;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.geminiService = geminiService;
    }

    /**
     * Lấy gợi ý AI Thổ Địa cho user
     */
    public LocalGuideResponse getSuggestions(Long userId, double lat, double lng) {
        // Check cache (24h per user)
        String today = LocalDate.now().toString();
        String cacheKey = userId + "_" + today;
        LocalGuideResponse cached = suggestionCache.get(cacheKey);
        if (cached != null) {
            log.info("Local Guide cache HIT for user {}", userId);
            return cached;
        }

        log.info("=== AI THỔ ĐỊA: Gợi ý cho user {} ===", userId);

        // BƯỚC 1: Thu thập context
        WeatherInfo weather = weatherService.getWeather(lat, lng);
        String city = weather.getCity();
        int month = LocalDate.now().getMonthValue();
        String region = detectRegion(city);

        // Lấy taste profile
        String tasteProfile = getUserTasteProfile(userId);

        // BƯỚC 2: Gọi Gemini
        List<GeminiSuggestion> geminiResults = callGeminiForSuggestions(city, month, region, tasteProfile);

        // BƯỚC 3 + 4: RAG match + build response
        LocalGuideResponse response = new LocalGuideResponse();
        response.setLocationLabel(city);
        response.setSeasonLabel(buildSeasonLabel(geminiResults));
        response.setTasteProfile(tasteProfile);

        List<SuggestionItem> items = new ArrayList<>();
        for (GeminiSuggestion gs : geminiResults) {
            SuggestionItem item = new SuggestionItem();
            item.setName(gs.name);
            item.setReason(gs.reason);
            item.setEmoji(gs.emoji);

            // RAG: vector search trên bài đăng
            List<Post> matched = searchPostsByRAG(gs.name);
            item.setMatchedPosts(matched);
            item.setHasResults(!matched.isEmpty());

            items.add(item);
        }
        response.setSuggestions(items);

        // Cache result
        suggestionCache.put(cacheKey, response);

        return response;
    }

    /**
     * BƯỚC 1b: Xây dựng/lấy hồ sơ khẩu vị
     */
    private String getUserTasteProfile(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "Đa dạng, Thích khám phá";

        // Nếu đã có taste profile → dùng luôn
        if (user.getTasteProfile() != null && !user.getTasteProfile().isEmpty()) {
            return user.getTasteProfile();
        }

        // Chưa có → tạo default dựa trên vùng miền (nếu có location)
        String defaultProfile = generateDefaultProfile(user);
        user.setTasteProfile(defaultProfile);
        userRepository.save(user);
        return defaultProfile;
    }

    private String generateDefaultProfile(User user) {
        // Default profile cơ bản — sẽ được update khi có lịch sử mua
        if (user.getLatitude() != null) {
            if (user.getLatitude() > 19) return "Món Bắc truyền thống, Phở, Bún";
            if (user.getLatitude() > 14) return "Đặc sản miền Trung, Cay, Hải sản";
            return "Trái cây nhiệt đới, Đặc sản miền Tây, Ngọt";
        }
        return "Đa dạng, Thích khám phá ẩm thực";
    }

    /**
     * Update taste profile từ lịch sử mua hàng (gọi ngầm sau khi hoàn thành đơn)
     */
    public void updateTasteProfileFromHistory(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // Lấy lịch sử items đã mua
        List<String> purchasedItems = getPurchasedItemNames(userId);
        if (purchasedItems.size() < 3) return; // Cần ít nhất 3 items

        // Gọi Gemini phân tích khẩu vị
        try {
            String prompt = "Dựa vào danh sách các món/nguyên liệu khách đã mua: [" +
                    String.join(", ", purchasedItems) +
                    "]. Hãy tóm tắt khẩu vị của người này trong đúng 3 từ khóa ngắn gọn, " +
                    "cách nhau bằng dấu phẩy. Chỉ trả về 3 từ khóa, không giải thích.";

            String result = callGeminiSimple(prompt);
            if (result != null && !result.isEmpty()) {
                user.setTasteProfile(result.trim());
                userRepository.save(user);
                log.info("Updated taste profile for user {}: {}", userId, result.trim());
                // Xóa cache để lần sau load lại
                String today = LocalDate.now().toString();
                suggestionCache.remove(userId + "_" + today);
            }
        } catch (Exception e) {
            log.warn("Failed to update taste profile: {}", e.getMessage());
        }
    }

    private List<String> getPurchasedItemNames(Long userId) {
        // Lấy items từ shopping requests đã hoàn thành
        try {
            return postRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .limit(10)
                    .map(Post::getTitle)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * BƯỚC 2: Gọi Gemini gợi ý 3 đặc sản
     */
    private List<GeminiSuggestion> callGeminiForSuggestions(String city, int month, String region, String tasteProfile) {
        try {
            String prompt = buildLocalGuidePrompt(city, month, region, tasteProfile);
            String response = callGeminiRaw(prompt);
            return parseGeminiSuggestions(response);
        } catch (Exception e) {
            log.error("Gemini local guide failed: {}", e.getMessage());
            return getDefaultSuggestions(month, region);
        }
    }

    private String buildLocalGuidePrompt(String city, int month, String region, String tasteProfile) {
        return String.format(
            "Bạn là chuyên gia ẩm thực và đặc sản địa phương Việt Nam (AI Thổ Địa). " +
            "Khách hàng đang ở %s (vùng %s) vào tháng %d. " +
            "Khẩu vị của họ: '%s'. " +
            "Hãy gợi ý đúng 3 món đặc sản địa phương hoặc trái cây/nông sản theo mùa phù hợp nhất. " +
            "Ưu tiên: (1) đang đúng mùa vụ tháng %d, (2) đặc sản vùng %s, (3) phù hợp khẩu vị. " +
            "Trả về CHÍNH XÁC JSON (không markdown, không ```json):\n" +
            "[{\"name\": \"Tên đặc sản\", \"reason\": \"Lý do gợi ý ngắn gọn\", \"emoji\": \"emoji phù hợp\"}]",
            city, region, month, tasteProfile, month, region
        );
    }

    private String callGeminiRaw(String prompt) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelName);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.8);
        requestBody.addProperty("max_tokens", 1024);

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("HTTP-Referer", "https://gomarket.vn")
                .addHeader("X-Title", "GoMarket")
                .post(body)
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            throw new RuntimeException("Gemini API error: " + response.code());
        }
    }

    private String callGeminiSimple(String prompt) throws Exception {
        String raw = callGeminiRaw(prompt);
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
        return root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString().trim();
    }

    private List<GeminiSuggestion> parseGeminiSuggestions(String response) {
        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            String text = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();

            text = text.replace("```json", "").replace("```", "").trim();
            JsonArray arr = JsonParser.parseString(text).getAsJsonArray();

            List<GeminiSuggestion> results = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                GeminiSuggestion gs = new GeminiSuggestion();
                gs.name = obj.get("name").getAsString();
                gs.reason = obj.get("reason").getAsString();
                gs.emoji = obj.has("emoji") ? obj.get("emoji").getAsString() : "🍽️";
                results.add(gs);
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to parse Gemini suggestions: {}", e.getMessage());
            return getDefaultSuggestions(LocalDate.now().getMonthValue(), "MIEN_NAM");
        }
    }

    /**
     * BƯỚC 3: RAG search bài đăng cho mỗi gợi ý
     */
    private List<Post> searchPostsByRAG(String suggestionName) {
        List<Post> results = new ArrayList<>();

        // Thử vector search trước
        try {
            float[] queryVector = embeddingService.embed(suggestionName);
            if (queryVector != null) {
                String vectorStr = Arrays.toString(queryVector);
                List<Post> vectorResults = postRepository.searchByVector(vectorStr, 5);
                if (!vectorResults.isEmpty()) {
                    vectorResults.forEach(this::enrichPost);
                    return vectorResults;
                }
            }
        } catch (Exception e) {
            log.debug("Vector search failed for '{}', trying text: {}", suggestionName, e.getMessage());
        }

        // Fallback: text search
        List<Post> textResults = postRepository.searchByText(suggestionName);
        if (!textResults.isEmpty()) {
            textResults = textResults.subList(0, Math.min(5, textResults.size()));
            textResults.forEach(this::enrichPost);
            return textResults;
        }

        return results;
    }

    private void enrichPost(Post post) {
        userRepository.findById(post.getUserId()).ifPresent(user -> {
            post.setAuthorName(user.getFullName());
            post.setAuthorAvatar(user.getAvatarUrl());
            post.setAuthorPhone(user.getPhone());
        });
    }

    // ═══ HELPERS ═══

    private String detectRegion(String city) {
        if (city == null) return "miền Nam";
        String lower = city.toLowerCase();
        // Miền Bắc
        if (lower.contains("hà nội") || lower.contains("hai phong") || lower.contains("hải phòng")
                || lower.contains("quang ninh") || lower.contains("nam dinh")) return "miền Bắc";
        // Miền Trung
        if (lower.contains("đà nẵng") || lower.contains("huế") || lower.contains("hue")
                || lower.contains("nha trang") || lower.contains("đà lạt") || lower.contains("dalat")
                || lower.contains("quy nhon") || lower.contains("buon ma thuot")) return "miền Trung";
        // Default miền Nam
        return "miền Nam";
    }

    private String buildSeasonLabel(List<GeminiSuggestion> suggestions) {
        if (suggestions.isEmpty()) return "Đặc sản theo mùa";
        if (suggestions.size() == 1) return "Mùa " + suggestions.get(0).name;
        return "Mùa " + suggestions.get(0).name + " & " + suggestions.get(1).name;
    }

    /** Gợi ý mặc định khi Gemini lỗi — dựa trên tháng và vùng miền */
    private List<GeminiSuggestion> getDefaultSuggestions(int month, String region) {
        List<GeminiSuggestion> defaults = new ArrayList<>();

        // Mùa vụ phổ biến theo tháng
        if (month >= 5 && month <= 8) {
            defaults.add(new GeminiSuggestion("Sầu riêng", "Đang vào chính vụ, ngon nhất trong năm", "🍈"));
            defaults.add(new GeminiSuggestion("Vải thiều", "Mùa vải thiều Bắc Giang", "🍒"));
            defaults.add(new GeminiSuggestion("Măng cụt", "Trái cây mùa hè miền Nam", "🍑"));
        } else if (month >= 9 && month <= 11) {
            defaults.add(new GeminiSuggestion("Bưởi da xanh", "Mùa bưởi chín, ngọt mát", "🍊"));
            defaults.add(new GeminiSuggestion("Cua gạch", "Mùa cua gạch béo ngậy", "🦀"));
            defaults.add(new GeminiSuggestion("Hồng xiêm", "Trái cây thu hoạch mùa thu", "🍐"));
        } else if (month >= 12 || month <= 2) {
            defaults.add(new GeminiSuggestion("Dưa hấu", "Trái cây Tết truyền thống", "🍉"));
            defaults.add(new GeminiSuggestion("Mứt Tết", "Mùa mứt gừng, mứt dừa, mứt me", "🍬"));
            defaults.add(new GeminiSuggestion("Bánh chưng", "Đặc sản Tết cổ truyền", "🍚"));
        } else {
            // Tháng 3-4
            defaults.add(new GeminiSuggestion("Thốt nốt", "Mùa thốt nốt An Giang bắt đầu", "🥥"));
            defaults.add(new GeminiSuggestion("Xoài cát Hòa Lộc", "Đầu mùa xoài, ngọt thơm béo", "🥭"));
            defaults.add(new GeminiSuggestion("Mít Thái", "Mít chín cây, thơm nức", "🍈"));
        }

        return defaults;
    }

    // Inner class for Gemini response
    static class GeminiSuggestion {
        String name;
        String reason;
        String emoji;

        GeminiSuggestion() {}

        GeminiSuggestion(String name, String reason, String emoji) {
            this.name = name;
            this.reason = reason;
            this.emoji = emoji;
        }
    }
}
