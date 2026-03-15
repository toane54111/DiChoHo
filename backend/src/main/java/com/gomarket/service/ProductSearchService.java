package com.gomarket.service;

import com.gomarket.dto.ProductSearchResult;
import com.gomarket.dto.RecipeResponse.ProductInfo;
import com.gomarket.model.Product;
import com.gomarket.repository.ProductRepository;
import com.gomarket.util.VietnameseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ProductSearchService - Tìm sản phẩm bằng RAG (Vector Search)
 *
 * Flow:
 * 1. Nhận tên nguyên liệu từ AI (VD: "thịt bò")
 * 2. Gọi EmbeddingService nhúng thành vector 768 chiều (Ollama nomic-embed-text)
 * 3. Dùng pgvector cosine distance (<=>) tìm sản phẩm gần nhất trong DB
 * 4. Kết hợp trọng số khoảng cách: shop gần user được ưu tiên hơn
 * 5. Lọc bỏ sản phẩm có similarity < 0.6 (tránh match nhầm)
 * 6. Trả về top 3 sản phẩm có điểm tổng hợp cao nhất
 */
@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    /** Ngưỡng similarity tối thiểu - dưới mức này coi như không khớp
     *  Với prefix search_document/search_query, nomic-embed-text phân biệt tốt → giữ 0.6 */
    private static final double MIN_SIMILARITY_THRESHOLD = 0.6;

    /** Trọng số: similarity chiếm 70%, khoảng cách chiếm 30% */
    private static final double WEIGHT_SIMILARITY = 0.7;
    private static final double WEIGHT_DISTANCE = 0.3;

    /** Khoảng cách tối đa tính điểm (km) - xa hơn thì điểm distance = 0 */
    private static final double MAX_DISTANCE_KM = 10.0;

    private final ProductRepository productRepository;
    private final EmbeddingService embeddingService;
    private final EntityManager entityManager;
    private final SynonymService synonymService;

    public ProductSearchService(ProductRepository productRepository,
                                 EmbeddingService embeddingService,
                                 EntityManager entityManager,
                                 SynonymService synonymService) {
        this.productRepository = productRepository;
        this.embeddingService = embeddingService;
        this.entityManager = entityManager;
        this.synonymService = synonymService;
    }

    /**
     * RAG Search: Tìm sản phẩm phù hợp nhất cho 1 nguyên liệu
     * Có tính trọng số khoảng cách từ user đến shop
     *
     * @param ingredientName tên nguyên liệu
     * @param userLat vĩ độ user (để tính khoảng cách)
     * @param userLng kinh độ user
     */
    public List<ProductInfo> searchForIngredient(String ingredientName, double userLat, double userLng) {
        log.info("RAG Search: Đang nhúng '{}' thành vector...", ingredientName);

        // Bước 1: Nhúng nguyên liệu thành vector
        float[] queryVector = embeddingService.embed(ingredientName);

        if (queryVector != null) {
            // Bước 2: Vector search + trọng số khoảng cách
            List<ProductInfo> results = vectorSearchWithDistance(queryVector, ingredientName, userLat, userLng);
            if (!results.isEmpty()) {
                log.info("RAG Search: Tìm được {} sản phẩm cho '{}'", results.size(), ingredientName);
                return results;
            }
        }

        // Fallback: Nếu embedding lỗi hoặc không tìm thấy → dùng text search
        log.warn("RAG Search fallback: Dùng text search cho '{}'", ingredientName);
        return textSearchFallback(ingredientName);
    }

    /**
     * Overload: search không có vị trí user (backward compatible)
     */
    public List<ProductInfo> searchForIngredient(String ingredientName) {
        return searchForIngredient(ingredientName, 0, 0);
    }

    /**
     * Vector Search kết hợp trọng số khoảng cách
     *
     * Công thức tính điểm tổng hợp:
     *   finalScore = similarity * 0.7 + distanceScore * 0.3
     *
     * Trong đó:
     *   - similarity = 1 - cosine_distance (từ pgvector)
     *   - distanceScore = 1 - (khoảng_cách_km / MAX_DISTANCE_KM)
     *     → Shop gần (500m) → score 0.95
     *     → Shop xa (5km)   → score 0.50
     *     → Shop rất xa (10km+) → score 0.00
     *
     * Ví dụ:
     *   Sản phẩm A: similarity=0.95, cách 10km → final = 0.95*0.7 + 0.0*0.3  = 0.665
     *   Sản phẩm B: similarity=0.90, cách 500m → final = 0.90*0.7 + 0.95*0.3 = 0.915  ← Ưu tiên!
     */
    @SuppressWarnings("unchecked")
    private List<ProductInfo> vectorSearchWithDistance(float[] queryVector, String ingredientName,
                                                        double userLat, double userLng) {
        try {
            String vectorStr = vectorToString(queryVector);

            // Query: lấy similarity + tọa độ shop để tính khoảng cách
            String sql = "SELECT p.id, p.name, p.price, p.original_price, p.unit, p.category, " +
                    "p.image_url, p.description, p.shop_id, " +
                    "(1 - (p.embedding <=> cast(:vector as vector))) as similarity, " +
                    "s.latitude as shop_lat, s.longitude as shop_lng, s.name as shop_name " +
                    "FROM products p " +
                    "LEFT JOIN shops s ON p.shop_id = s.id " +
                    "WHERE p.embedding IS NOT NULL " +
                    "ORDER BY p.embedding <=> cast(:vector as vector) ASC " +
                    "LIMIT 10"; // Lấy 10 rồi re-rank theo composite score

            List<Object[]> rows = entityManager.createNativeQuery(sql)
                    .setParameter("vector", vectorStr)
                    .getResultList();

            List<ProductInfo> results = new ArrayList<>();
            for (Object[] row : rows) {
                double similarity = row[9] != null ? ((Number) row[9]).doubleValue() : 0;

                // ─── Lọc ngưỡng similarity ───
                // Nếu similarity < 0.6 → sản phẩm không liên quan, bỏ qua
                if (similarity < MIN_SIMILARITY_THRESHOLD) {
                    log.debug("  ✗ Bỏ qua '{}' (similarity: {} < ngưỡng {})",
                            row[1], String.format("%.4f", similarity), MIN_SIMILARITY_THRESHOLD);
                    continue;
                }

                // ─── Tính trọng số khoảng cách ───
                double distanceScore = 1.0; // Mặc định nếu không có tọa độ
                double distanceKm = 0;
                if (userLat != 0 && userLng != 0 && row[10] != null && row[11] != null) {
                    double shopLat = ((Number) row[10]).doubleValue();
                    double shopLng = ((Number) row[11]).doubleValue();
                    distanceKm = haversine(userLat, userLng, shopLat, shopLng);
                    distanceScore = Math.max(0, 1.0 - (distanceKm / MAX_DISTANCE_KM));
                }

                // ─── Tính điểm tổng hợp ───
                double finalScore = similarity * WEIGHT_SIMILARITY + distanceScore * WEIGHT_DISTANCE;

                ProductInfo info = new ProductInfo();
                info.setId(((Number) row[0]).longValue());
                info.setName((String) row[1]);
                info.setPrice(row[2] != null ? ((Number) row[2]).doubleValue() : 0);
                info.setOriginal_price(row[3] != null ? ((Number) row[3]).doubleValue() : 0);
                info.setUnit((String) row[4]);
                info.setCategory((String) row[5]);
                info.setImage_url((String) row[6]);
                info.setDescription((String) row[7]);

                if (row[8] != null) {
                    info.setShop_id(((Number) row[8]).intValue());
                    info.setShop_name(row[12] != null ? (String) row[12] : "Unknown");
                }

                info.setSimilarity(finalScore);

                log.info("  → '{}' [sim={}, dist={}km, distScore={}, FINAL={}]",
                        info.getName(),
                        String.format("%.4f", similarity),
                        String.format("%.2f", distanceKm),
                        String.format("%.4f", distanceScore),
                        String.format("%.4f", finalScore));

                results.add(info);
            }

            // Sắp xếp theo điểm tổng hợp giảm dần
            results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

            // Trả về top 3
            return results.size() > 3 ? new ArrayList<>(results.subList(0, 3)) : results;
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Tìm sản phẩm cho tất cả nguyên liệu
     * Có truyền tọa độ user để tính trọng số khoảng cách
     */
    public List<ProductInfo> searchForIngredients(List<String> ingredientNames, double userLat, double userLng) {
        List<ProductInfo> allProducts = new ArrayList<>();
        for (String name : ingredientNames) {
            List<ProductInfo> matched = searchForIngredient(name, userLat, userLng);
            if (!matched.isEmpty()) {
                ProductInfo bestMatch = matched.get(0);

                // Nếu best match vẫn dưới ngưỡng → đánh dấu "cần tìm thay thế"
                if (bestMatch.getSimilarity() < MIN_SIMILARITY_THRESHOLD) {
                    log.warn("  ⚠ Nguyên liệu '{}' không tìm thấy sản phẩm phù hợp (best similarity: {})",
                            name, String.format("%.4f", bestMatch.getSimilarity()));
                    bestMatch.setDescription("⚠ Sản phẩm thay thế - không tìm thấy chính xác");
                }

                allProducts.add(bestMatch);
            } else {
                log.warn("  ⚠ Không tìm thấy sản phẩm nào cho nguyên liệu '{}'", name);
            }
        }
        return allProducts;
    }

    /**
     * Overload: backward compatible (không có tọa độ user)
     */
    public List<ProductInfo> searchForIngredients(List<String> ingredientNames) {
        return searchForIngredients(ingredientNames, 0, 0);
    }

    /**
     * Fallback: Text search khi vector search không khả dụng
     */
    private List<ProductInfo> textSearchFallback(String ingredientName) {
        List<Product> products = productRepository.searchByKeyword(ingredientName);

        List<ProductInfo> results = new ArrayList<>();
        for (Product p : products) {
            ProductInfo info = toProductInfo(p);
            info.setSimilarity(calculateTextSimilarity(ingredientName, p.getName()));
            results.add(info);
        }

        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        return results.size() > 3 ? results.subList(0, 3) : results;
    }

    /**
     * Công thức Haversine - tính khoảng cách 2 điểm trên trái đất (km)
     */
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371; // Bán kính trái đất (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private double calculateTextSimilarity(String query, String productName) {
        String q = query.toLowerCase().trim();
        String p = productName.toLowerCase().trim();
        if (p.contains(q)) return 0.9;
        if (q.contains(p)) return 0.8;
        String[] queryWords = q.split("\\s+");
        int matchCount = 0;
        for (String word : queryWords) {
            if (p.contains(word)) matchCount++;
        }
        return queryWords.length > 0 ? (double) matchCount / queryWords.length * 0.7 : 0;
    }

    /**
     * Hybrid Search: Kết hợp Text Search + Vector Search
     *
     * Bước 1 (Lexical): LIKE '%keyword%' → score = 1.0 (exact match)
     * Bước 2 (Semantic): RAG vector search → score = similarity (0.6 - 0.9)
     * Merge: deduplicate theo product ID, giữ score cao nhất
     * Sort: descending by score
     *
     * VD: "đồ nấu canh chua"
     *   - LIKE: 0 kết quả (không có sp nào tên "đồ nấu canh chua")
     *   - RAG:  Cà chua (0.85), Dứa (0.78), Giá đỗ (0.72), Cá lóc (0.68)...
     */
    public List<ProductSearchResult> hybridSearch(String query) {
        log.info("=== HYBRID SEARCH: '{}' ===", query);
        Map<Long, ProductSearchResult> resultMap = new LinkedHashMap<>();

        // ─── BƯỚC 0: Query Rewrite (Synonym) ───
        // "thịt lợn" → "thịt heo", "dứa" → "thơm", "mì chính" → "bột ngọt"
        List<String> queryVariants = synonymService.getQueryVariants(query);
        String rewrittenQuery = synonymService.rewriteQuery(query);
        log.info("  Query Rewrite: '{}' → variants={}", query, queryVariants);

        // ─── BƯỚC 1: Text Search (LIKE có dấu + không dấu) → score = 1.0 / 0.95 ───
        for (String variant : queryVariants) {
            // 1a: LIKE có dấu (chính xác)
            List<Product> textResults = productRepository.searchByKeyword(variant);
            for (Product p : textResults) {
                if (resultMap.containsKey(p.getId())) continue;
                ProductSearchResult r = toSearchResult(p);
                r.setScore(1.0);
                r.setMatchType("exact");
                resultMap.put(p.getId(), r);
            }
        }
        log.info("  Text search (có dấu): {} kết quả", resultMap.size());

        // 1b: Search không dấu - duyệt tất cả sản phẩm, so sánh tên đã bỏ dấu
        List<Product> allProducts = productRepository.findAll();
        for (String variant : queryVariants) {
            for (Product p : allProducts) {
                if (resultMap.containsKey(p.getId())) continue;

                boolean nameMatch = VietnameseUtils.containsIgnoreDiacritics(p.getName(), variant);
                boolean categoryMatch = p.getCategory() != null &&
                        VietnameseUtils.containsIgnoreDiacritics(p.getCategory(), variant);

                if (nameMatch || categoryMatch) {
                    ProductSearchResult r = toSearchResult(p);
                    r.setScore(0.95);
                    r.setMatchType("exact");
                    resultMap.put(p.getId(), r);
                }
            }
        }
        log.info("  Text search (tổng cộng): {} kết quả", resultMap.size());

        // ─── BƯỚC 2: Vector Search (RAG) → score = similarity ───
        // Dùng rewrittenQuery để embed (đã chuẩn hóa synonym)
        try {
            float[] queryVector = embeddingService.embed(rewrittenQuery);
            if (queryVector != null) {
                List<ProductSearchResult> vectorResults = vectorSearchForHybrid(queryVector);
                log.info("  Vector search: {} kết quả cho '{}'", vectorResults.size(), rewrittenQuery);

                for (ProductSearchResult vr : vectorResults) {
                    if (resultMap.containsKey(vr.getId())) {
                        log.debug("  Duplicate ID={} (giữ score text)", vr.getId());
                    } else {
                        resultMap.put(vr.getId(), vr);
                    }
                }
            } else {
                log.warn("  Vector search: Embedding failed, chỉ dùng text search");
            }
        } catch (Exception e) {
            log.error("  Vector search failed: {}", e.getMessage());
        }

        // ─── Sort by score DESC ───
        List<ProductSearchResult> results = new ArrayList<>(resultMap.values());
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        log.info("=== HYBRID SEARCH DONE: {} kết quả (text={}, semantic={}) ===",
                results.size(),
                results.stream().filter(r -> "exact".equals(r.getMatchType())).count(),
                results.stream().filter(r -> "semantic".equals(r.getMatchType())).count());

        return results;
    }

    /**
     * Vector search trả về ProductSearchResult (cho hybrid search)
     * Không giới hạn top 3 như searchForIngredient, lấy tất cả > threshold
     */
    @SuppressWarnings("unchecked")
    private List<ProductSearchResult> vectorSearchForHybrid(float[] queryVector) {
        String vectorStr = vectorToString(queryVector);

        String sql = "SELECT p.id, p.name, p.price, p.original_price, p.unit, p.category, " +
                "p.image_url, p.description, p.shop_id, " +
                "(1 - (p.embedding <=> cast(:vector as vector))) as similarity, " +
                "s.name as shop_name " +
                "FROM products p " +
                "LEFT JOIN shops s ON p.shop_id = s.id " +
                "WHERE p.embedding IS NOT NULL " +
                "ORDER BY p.embedding <=> cast(:vector as vector) ASC " +
                "LIMIT 20";

        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("vector", vectorStr)
                .getResultList();

        List<ProductSearchResult> results = new ArrayList<>();
        for (Object[] row : rows) {
            double similarity = row[9] != null ? ((Number) row[9]).doubleValue() : 0;

            if (similarity < MIN_SIMILARITY_THRESHOLD) {
                continue;
            }

            ProductSearchResult r = new ProductSearchResult();
            r.setId(((Number) row[0]).longValue());
            r.setName((String) row[1]);
            r.setPrice(row[2] != null ? ((Number) row[2]).doubleValue() : 0);
            r.setOriginalPrice(row[3] != null ? ((Number) row[3]).doubleValue() : 0);
            r.setUnit((String) row[4]);
            r.setCategory((String) row[5]);
            r.setImageUrl((String) row[6]);
            r.setDescription((String) row[7]);
            r.setShopId(row[8] != null ? ((Number) row[8]).intValue() : null);
            r.setScore(similarity);
            r.setMatchType("semantic");
            r.setShopName(row[10] != null ? (String) row[10] : null);

            log.info("    RAG: '{}' [similarity={}]", r.getName(), String.format("%.4f", similarity));
            results.add(r);
        }

        return results;
    }

    private ProductSearchResult toSearchResult(Product product) {
        ProductSearchResult r = new ProductSearchResult();
        r.setId(product.getId());
        r.setName(product.getName());
        r.setPrice(product.getPrice() != null ? product.getPrice() : 0);
        r.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : 0);
        r.setUnit(product.getUnit());
        r.setCategory(product.getCategory());
        r.setImageUrl(product.getImageUrl());
        r.setDescription(product.getDescription());
        if (product.getShop() != null) {
            r.setShopId(product.getShop().getId().intValue());
            r.setShopName(product.getShop().getName());
        }
        return r;
    }

    private ProductInfo toProductInfo(Product product) {
        ProductInfo info = new ProductInfo();
        info.setId(product.getId());
        info.setName(product.getName());
        info.setPrice(product.getPrice() != null ? product.getPrice() : 0);
        info.setOriginal_price(product.getOriginalPrice() != null ? product.getOriginalPrice() : 0);
        info.setUnit(product.getUnit());
        info.setCategory(product.getCategory());
        info.setImage_url(product.getImageUrl());
        info.setDescription(product.getDescription());
        if (product.getShop() != null) {
            info.setShop_id(product.getShop().getId().intValue());
            info.setShop_name(product.getShop().getName());
        }
        return info;
    }
}
