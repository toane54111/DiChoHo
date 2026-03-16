package com.gomarket.controller;

import com.gomarket.dto.RecipeRequest;
import com.gomarket.dto.RecipeResponse;
import com.gomarket.dto.RecipeResponse.*;
import com.gomarket.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * RecipeController - Gợi ý món ăn theo thời tiết (đơn giản hóa, bỏ RAG/Route)
 *
 * Flow:
 * 1. App gửi GPS (lat/lng) → Backend
 * 2. Backend → OpenWeather API (lấy thời tiết)
 * 3. Backend → Gemini LLM (gợi ý món ăn dựa trên thời tiết)
 * 4. Backend → Tìm ảnh món ăn
 * 5. Backend → App (trả về weather + recipe)
 */
@RestController
@RequestMapping("/api/recipe")
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);

    private final WeatherService weatherService;
    private final GeminiService geminiService;
    private final ImageSearchService imageSearchService;
    private final ShoppingRequestService shoppingRequestService;

    public RecipeController(WeatherService weatherService,
                            GeminiService geminiService,
                            ImageSearchService imageSearchService,
                            ShoppingRequestService shoppingRequestService) {
        this.weatherService = weatherService;
        this.geminiService = geminiService;
        this.imageSearchService = imageSearchService;
        this.shoppingRequestService = shoppingRequestService;
    }

    /** GET /api/recipe/weather — Lấy thời tiết nhanh */
    @GetMapping("/weather")
    public ResponseEntity<WeatherInfo> getWeather(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        WeatherInfo weather = weatherService.getWeather(latitude, longitude);
        return ResponseEntity.ok(weather);
    }

    /**
     * POST /api/recipe/suggest — Gợi ý món ăn (đơn giản, không RAG/Route)
     */
    @PostMapping("/suggest")
    public ResponseEntity<RecipeResponse> suggestRecipe(@RequestBody RecipeRequest request) {
        log.info("=== GỢI Ý MÓN ĂN (simplified) ===");

        RecipeResponse response = new RecipeResponse();

        // Step 1: Lấy thời tiết
        WeatherInfo weather = weatherService.getWeather(
                request.getLatitude(), request.getLongitude());
        response.setWeather(weather);

        // Step 2: Gọi Gemini gợi ý món
        String weatherSummary = weatherService.getWeatherSummary(weather);
        List<String> excludeDishes = request.getExcludeDishes();
        RecipeInfo recipe = geminiService.suggestRecipe(weatherSummary, excludeDishes);
        response.setRecipe(recipe);

        // Step 3: Tìm ảnh món ăn
        String dishImageUrl = imageSearchService.searchDishImage(recipe.getName());
        if (dishImageUrl != null) {
            recipe.setImage_url(dishImageUrl);
        }

        // Không còn RAG search sản phẩm, không còn route
        response.setProducts(new ArrayList<>());
        response.setAlternative_recipes(new ArrayList<>());

        log.info("Gợi ý: {} ({}°C, {})", recipe.getName(), weather.getTemp(), weather.getDescription());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/recipe/to-shopping-request — Tạo đơn đi chợ hộ từ công thức
     * Body: { "userId": 1, "ingredients": ["2kg thịt heo", "1 bắp cải"...], "deliveryAddress": "...", "latitude": ..., "longitude": ... }
     */
    @PostMapping("/to-shopping-request")
    public ResponseEntity<?> recipeToShoppingRequest(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> ingredients = (List<String>) body.get("ingredients");

            // Chuyển ingredients thành items format cho ShoppingRequestService
            List<Map<String, String>> items = new ArrayList<>();
            for (String ingredient : ingredients) {
                items.add(Map.of("itemText", ingredient, "quantityNote", ""));
            }

            Map<String, Object> requestBody = new HashMap<>(body);
            requestBody.put("items", items);
            requestBody.putIfAbsent("notes", "Đơn từ AI Chef - gợi ý nấu ăn");

            var shoppingRequest = shoppingRequestService.createRequest(requestBody);
            return ResponseEntity.ok(shoppingRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
