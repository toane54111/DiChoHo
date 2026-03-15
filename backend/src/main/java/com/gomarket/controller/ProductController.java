package com.gomarket.controller;

import com.gomarket.dto.ProductSearchResult;
import com.gomarket.dto.ProductSuggestion;
import com.gomarket.model.Product;
import com.gomarket.repository.ProductRepository;
import com.gomarket.service.AutocompleteService;
import com.gomarket.service.ProductSearchService;
import com.gomarket.service.ScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ScraperService scraperService;
    private final ProductSearchService productSearchService;
    private final AutocompleteService autocompleteService;

    public ProductController(ProductRepository productRepository, ScraperService scraperService,
                             ProductSearchService productSearchService,
                             AutocompleteService autocompleteService) {
        this.productRepository = productRepository;
        this.scraperService = scraperService;
        this.productSearchService = productSearchService;
        this.autocompleteService = autocompleteService;
    }

    /**
     * GET /api/products/search?q=keyword
     * Tìm kiếm sản phẩm theo từ khóa (text only - legacy)
     */
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam("q") String query) {
        List<Product> products = productRepository.searchByKeyword(query);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/hybrid-search?q=keyword
     * Hybrid Search: Text (LIKE, score=1.0) + Vector/RAG (semantic, score=0.6-0.9)
     *
     * VD: "đồ nấu canh chua" → LIKE: 0 kết quả, RAG: Cà chua, Dứa, Giá đỗ...
     * VD: "thịt bò" → LIKE: Thịt bò Úc (1.0), RAG: Thịt bò NZ (0.88), Bắp bò (0.75)...
     */
    @GetMapping("/hybrid-search")
    public ResponseEntity<List<ProductSearchResult>> hybridSearch(@RequestParam("q") String query) {
        List<ProductSearchResult> results = productSearchService.hybridSearch(query);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/products/autocomplete?q=th
     * Autocomplete realtime - gợi ý sản phẩm khi user đang gõ
     * Serve từ RAM cache → response < 1ms
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<ProductSuggestion>> autocomplete(@RequestParam("q") String query) {
        List<ProductSuggestion> suggestions = autocompleteService.suggest(query);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * GET /api/products/category/{category}
     * Lấy sản phẩm theo danh mục
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getByCategory(@PathVariable String category) {
        List<Product> products = productRepository.findByCategory(category);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/by-category?name=Thịt heo
     * Lấy sản phẩm theo danh mục (dùng query param, tránh lỗi URL encode với ký tự đặc biệt)
     */
    @GetMapping("/by-category")
    public ResponseEntity<List<Product>> getByCategoryQuery(@RequestParam("name") String category) {
        List<Product> products = productRepository.findByCategory(category);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/categories
     * Lấy danh sách tất cả categories có trong DB
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = productRepository.findAll().stream()
                .map(Product::getCategory)
                .distinct()
                .sorted()
                .toList();
        return ResponseEntity.ok(categories);
    }

    /**
     * GET /api/products
     * Lấy tất cả sản phẩm
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    /**
     * GET /api/products/{id}
     * Lấy chi tiết sản phẩm theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/products/scrape
     * Trigger scraping dữ liệu từ Bách Hóa Xanh
     */
    @PostMapping("/scrape")
    public ResponseEntity<?> triggerScrape() {
        try {
            int count = scraperService.scrapeAll();
            return ResponseEntity.ok(Map.of(
                    "message", "Scraping hoàn tất",
                    "products_scraped", count
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Scraping thất bại: " + e.getMessage()));
        }
    }
}
