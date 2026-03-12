package com.gomarket.controller;

import com.gomarket.dto.RecipeRequest;
import com.gomarket.dto.RecipeResponse;
import com.gomarket.dto.RecipeResponse.*;
import com.gomarket.model.Product;
import com.gomarket.model.Shop;
import com.gomarket.repository.ShopRepository;
import com.gomarket.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RecipeController - Core API thực hiện flow chính của ứng dụng:
 *
 * Sequence Diagram Flow:
 * 1. App gửi GPS (lat/lng) → Backend
 * 2. Backend → OpenWeather API (lấy thời tiết)
 * 3. Backend → Gemini LLM (gợi ý món ăn dựa trên thời tiết)
 * 4. Backend → RAG Search trong DB (tìm nguyên liệu phù hợp)
 * 5. Backend → A* Routing (tìm lộ trình tối ưu qua các shop)
 * 6. Backend → App (trả về RecipeResponse đầy đủ)
 */
@RestController
@RequestMapping("/api/recipe")
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);

    private final WeatherService weatherService;
    private final GeminiService geminiService;
    private final ProductSearchService productSearchService;
    private final RouteService routeService;
    private final ShopRepository shopRepository;
    private final ImageSearchService imageSearchService;

    public RecipeController(WeatherService weatherService,
                            GeminiService geminiService,
                            ProductSearchService productSearchService,
                            RouteService routeService,
                            ShopRepository shopRepository,
                            ImageSearchService imageSearchService) {
        this.weatherService = weatherService;
        this.geminiService = geminiService;
        this.productSearchService = productSearchService;
        this.routeService = routeService;
        this.shopRepository = shopRepository;
        this.imageSearchService = imageSearchService;
    }

    /**
     * GET /api/recipe/weather - Lấy thời tiết nhanh (hiển thị trước khi gợi ý món)
     */
    @GetMapping("/weather")
    public ResponseEntity<WeatherInfo> getWeather(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        log.info("Lấy thời tiết: lat={}, lng={}", latitude, longitude);
        WeatherInfo weather = weatherService.getWeather(latitude, longitude);
        return ResponseEntity.ok(weather);
    }

    /**
     * POST /api/recipe/suggest
     * Core endpoint - Nhận GPS, trả về gợi ý món ăn + nguyên liệu + lộ trình
     */
    @PostMapping("/suggest")
    public ResponseEntity<RecipeResponse> suggestRecipe(@RequestBody RecipeRequest request) {
        log.info("=== BẮT ĐẦU GỢI Ý MÓN ĂN ===");
        log.info("Tọa độ: lat={}, lng={}", request.getLatitude(), request.getLongitude());

        RecipeResponse response = new RecipeResponse();

        // ─── STEP 1: Lấy thời tiết từ OpenWeather API ───
        log.info("Step 1: Đang lấy thông tin thời tiết...");
        WeatherInfo weather = weatherService.getWeather(
                request.getLatitude(), request.getLongitude()
        );
        response.setWeather(weather);
        log.info("Thời tiết: {}°C, {}", weather.getTemp(), weather.getDescription());

        // ─── STEP 2: Gọi Gemini LLM gợi ý món ăn ───
        log.info("Step 2: Đang gọi Gemini AI gợi ý món ăn...");
        String weatherSummary = weatherService.getWeatherSummary(weather);
        List<String> excludeDishes = request.getExcludeDishes();
        RecipeInfo recipe = geminiService.suggestRecipe(weatherSummary, excludeDishes);
        response.setRecipe(recipe);
        log.info("Món ăn gợi ý: {}", recipe.getName());

        // ─── STEP 2.5: Search ảnh món ăn từ Unsplash ───
        log.info("Step 2.5: Đang tìm ảnh món ăn...");
        String dishImageUrl = imageSearchService.searchDishImage(recipe.getName());
        if (dishImageUrl != null) {
            recipe.setImage_url(dishImageUrl);
            log.info("Ảnh món ăn: {}", dishImageUrl);
        }

        // ─── STEP 3: RAG Search - Tìm nguyên liệu trong DB ───
        log.info("Step 3: Đang tìm nguyên liệu phù hợp trong database (RAG Search)...");
        List<String> ingredientNames = recipe.getIngredients().stream()
                .map(IngredientInfo::getName)
                .collect(Collectors.toList());

        List<ProductInfo> matchedProducts = productSearchService.searchForIngredients(
                ingredientNames, request.getLatitude(), request.getLongitude()
        );
        response.setProducts(matchedProducts);
        log.info("Tìm được {} sản phẩm phù hợp", matchedProducts.size());

        // Map sản phẩm tìm được vào nguyên liệu tương ứng
        mapProductsToIngredients(recipe.getIngredients(), matchedProducts);

        // Tính tổng chi phí ước tính
        double totalCost = matchedProducts.stream()
                .mapToDouble(ProductInfo::getPrice)
                .sum();
        recipe.setTotal_cost(totalCost);

        // ─── STEP 4: A* Routing - Tìm lộ trình tối ưu ───
        log.info("Step 4: Đang tính lộ trình tối ưu (A* / Nearest Neighbor TSP)...");
        List<Shop> shopsToVisit = getShopsFromProducts(matchedProducts);
        RouteInfo route = routeService.findOptimalRoute(
                request.getLatitude(), request.getLongitude(), shopsToVisit
        );
        response.setRoute(route);
        log.info("Lộ trình: {} shops, {}km, ~{} phút",
                route.getShops().size(), route.getTotal_distance(), route.getEstimated_time());

        // ─── Bonus: Món ăn thay thế (hardcoded cho demo) ───
        response.setAlternative_recipes(new ArrayList<>());

        log.info("=== HOÀN THÀNH GỢI Ý MÓN ĂN ===");
        return ResponseEntity.ok(response);
    }

    /**
     * Map sản phẩm đã tìm được vào từng nguyên liệu tương ứng
     */
    private void mapProductsToIngredients(List<IngredientInfo> ingredients, List<ProductInfo> products) {
        for (IngredientInfo ingredient : ingredients) {
            // Tìm sản phẩm best match cho nguyên liệu này
            for (ProductInfo product : products) {
                String ingName = ingredient.getName().toLowerCase();
                String prodName = product.getName().toLowerCase();
                if (prodName.contains(ingName) || ingName.contains(prodName)) {
                    ingredient.setProduct(product);
                    break;
                }
            }
        }
    }

    /**
     * Lấy danh sách Shop từ danh sách sản phẩm đã match
     */
    private List<Shop> getShopsFromProducts(List<ProductInfo> products) {
        Set<Long> shopIds = new HashSet<>();
        for (ProductInfo product : products) {
            if (product.getShop_id() > 0) {
                shopIds.add((long) product.getShop_id());
            }
        }

        List<Shop> shops = new ArrayList<>();
        for (Long shopId : shopIds) {
            shopRepository.findById(shopId).ifPresent(shops::add);
        }

        return shops;
    }
}
