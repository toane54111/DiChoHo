package com.gomarket.service;

import com.gomarket.dto.RecipeResponse.RecipeInfo;
import com.gomarket.dto.RecipeResponse.IngredientInfo;
import com.google.gson.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * GeminiService - Gọi LLM để gợi ý món ăn dựa trên thời tiết
 *
 * Sử dụng OpenRouter API (tương thích OpenAI format)
 * Model: google/gemini-2.0-flash-001 (miễn phí, nhanh, hỗ trợ tiếng Việt)
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${api.openrouter.key}")
    private String apiKey;

    @Value("${api.openrouter.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    @Value("${api.openrouter.model:google/gemini-2.0-flash-001}")
    private String modelName;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    public RecipeInfo suggestRecipe(String weatherSummary, List<String> excludeDishes) {
        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String prompt = buildPrompt(weatherSummary, excludeDishes);
                String response = callOpenRouter(prompt);
                return parseRecipeFromResponse(response, weatherSummary);
            } catch (Exception e) {
                log.error("OpenRouter API lần {}/{} thất bại: {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    log.warn("Đợi 2 giây rồi thử lại...");
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    break;
                }
            }
        }
        log.warn("OpenRouter API thất bại, dùng món mặc định.");
        return getDefaultRecipe(weatherSummary);
    }

    private String buildPrompt(String weatherSummary, List<String> excludeDishes) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format(
            "Bạn là đầu bếp AI chuyên ẩm thực Việt Nam. " +
            "Hãy TÌM KIẾM TRÊN WEB công thức món ăn Việt Nam phù hợp với thời tiết: [%s].\n\n" +
            "YÊU CẦU BẮT BUỘC:\n" +
            "1. PHẢI search web để tìm công thức CHUẨN, THỰC TẾ từ các trang ẩm thực Việt Nam (cookpad, điện máy xanh, cooky.vn, beptruong.edu.vn).\n" +
            "2. Chọn món ăn PHÙ HỢP THỜI TIẾT:\n" +
            "   - Nóng/oi bức → món mát: gỏi, bún, salad, chè\n" +
            "   - Lạnh/mưa → món nóng: lẩu, phở, canh, cháo\n" +
            "   - Mát mẻ → món đa dạng: cơm, bún, hủ tiếu\n" +
            "3. Nguyên liệu PHẢI có bán tại siêu thị và chợ Việt Nam (Bách Hóa Xanh, VinMart, chợ truyền thống).\n" +
            "4. Tên nguyên liệu NGẮN GỌN, dùng tên thông dụng người Việt hay dùng:\n" +
            "   ✓ Đúng: 'thịt bò', 'rau muống', 'nấm kim châm', 'tôm sú', 'cà chua', 'hành tây'\n" +
            "   ✗ Sai: 'thịt bò Wagyu', 'nấm truffle', 'rau rocket', 'tôm hùm'\n" +
            "5. Từ 5-8 nguyên liệu chính (KHÔNG liệt kê gia vị cơ bản như muối, đường, dầu ăn, tiêu).\n" +
            "6. Các bước nấu phải CHI TIẾT, đúng kỹ thuật nấu ăn Việt Nam.\n\n",
            weatherSummary
        ));

        if (excludeDishes != null && !excludeDishes.isEmpty()) {
            prompt.append("QUAN TRỌNG: KHÔNG được gợi ý các món sau (đã gợi ý trước đó): ");
            prompt.append(String.join(", ", excludeDishes));
            prompt.append(". Hãy chọn món KHÁC hoàn toàn.\n\n");
        }

        prompt.append("6. ƯỚC TÍNH GIÁ: Mỗi nguyên liệu PHẢI có estimated_price (đơn vị VNĐ), " +
            "ước tính giá bán lẻ tại siêu thị/chợ Việt Nam theo số lượng đã nêu. " +
            "Ví dụ: 300g tôm sú ~ 75000, 200g nấm kim châm ~ 15000.\n\n");

        prompt.append("Trả về CHÍNH XÁC JSON (không markdown, không ```json):\n" +
            "{\"name\": \"Tên món\", \"description\": \"Mô tả ngắn\", " +
            "\"ingredients\": [{\"name\": \"tên nguyên liệu\", \"quantity\": \"số lượng\", \"estimated_price\": 50000}], " +
            "\"steps\": [\"Bước 1...\", \"Bước 2...\"]}");

        return prompt.toString();
    }

    /**
     * Gọi OpenRouter API (OpenAI-compatible format)
     * POST https://openrouter.ai/api/v1/chat/completions
     * Header: Authorization: Bearer sk-or-...
     */
    private String callOpenRouter(String prompt) throws Exception {
        // Build request body theo OpenAI format
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelName);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        requestBody.add("messages", messages);

        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 2048);

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("HTTP-Referer", "https://gomarket.vn")
                .addHeader("X-Title", "GoMarket")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            String errorBody = response.body() != null ? response.body().string() : "no body";
            throw new RuntimeException("OpenRouter API error: " + response.code() + " - " + errorBody);
        }
    }

    /**
     * Parse response từ OpenRouter (OpenAI format)
     * Response: { "choices": [{ "message": { "content": "..." } }] }
     */
    private RecipeInfo parseRecipeFromResponse(String response, String weatherSummary) {
        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            String text = choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            // Clean markdown nếu có
            text = text.replace("```json", "").replace("```", "").trim();

            JsonObject recipeJson = JsonParser.parseString(text).getAsJsonObject();

            RecipeInfo recipe = new RecipeInfo();
            recipe.setName(recipeJson.get("name").getAsString());
            recipe.setDescription(recipeJson.get("description").getAsString());
            recipe.setWeather_context(weatherSummary);

            // Parse ingredients
            List<IngredientInfo> ingredients = new ArrayList<>();
            double totalCost = 0;
            JsonArray ingArray = recipeJson.getAsJsonArray("ingredients");
            for (JsonElement el : ingArray) {
                JsonObject ingObj = el.getAsJsonObject();
                IngredientInfo ing = new IngredientInfo();
                ing.setName(ingObj.get("name").getAsString());
                ing.setQuantity(ingObj.get("quantity").getAsString());
                if (ingObj.has("estimated_price") && !ingObj.get("estimated_price").isJsonNull()) {
                    double price = ingObj.get("estimated_price").getAsDouble();
                    ing.setEstimated_price(price);
                    totalCost += price;
                }
                ingredients.add(ing);
            }
            recipe.setIngredients(ingredients);
            recipe.setTotal_cost(totalCost);

            // Parse steps
            List<String> steps = new ArrayList<>();
            JsonArray stepsArray = recipeJson.getAsJsonArray("steps");
            for (JsonElement el : stepsArray) {
                steps.add(el.getAsString());
            }
            recipe.setSteps(steps);

            return recipe;
        } catch (Exception e) {
            log.error("Failed to parse OpenRouter response: {}", e.getMessage());
            log.debug("Raw response: {}", response);
            return getDefaultRecipe(weatherSummary);
        }
    }

    private RecipeInfo getDefaultRecipe(String weatherSummary) {
        RecipeInfo recipe = new RecipeInfo();
        recipe.setName("Lẩu Thái Hải Sản");
        recipe.setDescription("Món lẩu đậm đà, cay nồng với nước dùng chua cay đặc trưng Thái Lan.");
        recipe.setWeather_context(weatherSummary);

        List<IngredientInfo> ingredients = new ArrayList<>();
        double totalCost = 0;
        Object[][] data = {
                {"Tôm sú", "300g", 75000.0}, {"Mực ống", "200g", 60000.0}, {"Nấm kim châm", "150g", 15000.0},
                {"Rau muống", "200g", 8000.0}, {"Sả", "3 cây", 5000.0}, {"Lá chanh", "5 lá", 3000.0},
                {"Ớt hiểm", "3 quả", 5000.0}, {"Nước cốt dừa", "200ml", 15000.0}
        };
        for (Object[] d : data) {
            IngredientInfo ing = new IngredientInfo();
            ing.setName((String) d[0]);
            ing.setQuantity((String) d[1]);
            ing.setEstimated_price((double) d[2]);
            totalCost += (double) d[2];
            ingredients.add(ing);
        }
        recipe.setIngredients(ingredients);
        recipe.setTotal_cost(totalCost);

        List<String> steps = List.of(
                "Sơ chế tôm, mực. Rửa sạch rau và nấm.",
                "Đập dập sả, thái lát. Xé nhỏ lá chanh.",
                "Đun nước dùng với sả, lá chanh, ớt, nước cốt dừa.",
                "Nêm nếm gia vị: nước mắm, đường, chanh.",
                "Cho tôm, mực vào nồi lẩu. Nhúng rau và nấm ăn kèm."
        );
        recipe.setSteps(steps);

        return recipe;
    }
}
