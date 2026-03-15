package com.gomarket.service;

import com.gomarket.dto.ProductSuggestion;
import com.gomarket.model.Product;
import com.gomarket.repository.ProductRepository;
import com.gomarket.util.VietnameseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Autocomplete Service - Gợi ý sản phẩm realtime khi user gõ
 *
 * Chiến lược: Load toàn bộ 1124 sản phẩm vào RAM khi khởi động.
 * Mỗi lần user gõ → filter trên RAM bằng Java Stream → ~0.1ms response.
 *
 * Tên SP được normalize (bỏ dấu) sẵn để so sánh nhanh.
 * Hỗ trợ: không dấu ("thit" → "Thịt heo"), synonym ("lợn" → "heo")
 */
@Service
public class AutocompleteService {

    private static final Logger log = LoggerFactory.getLogger(AutocompleteService.class);
    private static final int DEFAULT_LIMIT = 8;

    private final ProductRepository productRepository;
    private final SynonymService synonymService;

    // RAM cache: product suggestion + tên đã normalize
    private List<CachedProduct> cache = new ArrayList<>();

    public AutocompleteService(ProductRepository productRepository, SynonymService synonymService) {
        this.productRepository = productRepository;
        this.synonymService = synonymService;
    }

    /**
     * Load tất cả sản phẩm vào RAM cache
     * Gọi từ DataInitializer sau khi import xong
     */
    public void refreshCache() {
        List<Product> allProducts = productRepository.findAll();

        cache = allProducts.stream()
                .map(p -> new CachedProduct(
                        new ProductSuggestion(
                                p.getId(),
                                p.getName(),
                                p.getImageUrl(),
                                p.getPrice(),
                                p.getCategory()
                        ),
                        VietnameseUtils.removeDiacritics(p.getName()),
                        VietnameseUtils.removeDiacritics(p.getCategory() != null ? p.getCategory() : "")
                ))
                .collect(Collectors.toList());

        log.info("Autocomplete cache loaded: {} sản phẩm", cache.size());
    }

    /**
     * Gợi ý sản phẩm theo query
     * Hỗ trợ: không dấu, synonym, partial match
     *
     * @param query từ khóa user đang gõ
     * @param limit số kết quả tối đa
     * @return danh sách gợi ý (nhẹ, chỉ id+name+image+price+category)
     */
    public List<ProductSuggestion> suggest(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return new ArrayList<>();
        }

        // Synonym rewrite: "thit lon" → "thịt heo" → normalize → "thit heo"
        List<String> variants = synonymService.getQueryVariants(query);
        List<String> normalizedVariants = variants.stream()
                .map(VietnameseUtils::removeDiacritics)
                .distinct()
                .collect(Collectors.toList());

        int maxResults = limit > 0 ? limit : DEFAULT_LIMIT;

        // Filter trên RAM - cực nhanh
        return cache.stream()
                .filter(cp -> {
                    for (String nq : normalizedVariants) {
                        if (cp.normalizedName.contains(nq) || cp.normalizedCategory.contains(nq)) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(maxResults)
                .map(cp -> cp.suggestion)
                .collect(Collectors.toList());
    }

    public List<ProductSuggestion> suggest(String query) {
        return suggest(query, DEFAULT_LIMIT);
    }

    /**
     * Cache entry: suggestion DTO + tên đã normalize sẵn
     */
    private static class CachedProduct {
        final ProductSuggestion suggestion;
        final String normalizedName;
        final String normalizedCategory;

        CachedProduct(ProductSuggestion suggestion, String normalizedName, String normalizedCategory) {
            this.suggestion = suggestion;
            this.normalizedName = normalizedName;
            this.normalizedCategory = normalizedCategory;
        }
    }
}
