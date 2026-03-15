package com.gomarket.service;

import com.gomarket.model.Product;
import com.gomarket.model.Shop;
import com.gomarket.repository.ProductRepository;
import com.gomarket.repository.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gomarket.repository.OrderRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;

    public ScraperService(ProductRepository productRepository, ShopRepository shopRepository,
                          OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Import dữ liệu Bách Hóa Xanh từ file CSV trong resources/data/
     * Mỗi file CSV = 1 category (tên file = tên category)
     * Xóa data cũ trước khi import
     */
    @Transactional
    public void seedSampleData() {
        long existingCount = productRepository.count();

        // Kiểm tra xem data đã dùng ảnh local chưa
        boolean needReimport = false;
        if (existingCount > 0) {
            // Nếu còn sản phẩm với URL CDN cũ → cần re-import với ảnh local
            long cdnCount = productRepository.findAll().stream()
                    .filter(p -> p.getImageUrl() != null && p.getImageUrl().startsWith("http"))
                    .count();
            if (cdnCount > 0) {
                log.info("Phát hiện {} sản phẩm dùng ảnh CDN cũ, re-import với ảnh local...", cdnCount);
                needReimport = true;
            } else if (existingCount >= 200) {
                log.info("Database đã có {} sản phẩm (ảnh local), bỏ qua import.", existingCount);
                return;
            }
        }

        // Xóa data cũ để import data BHX mới với ảnh local
        // Xóa theo thứ tự FK: orders → products → shops
        if (existingCount > 0) {
            log.info("Xóa {} sản phẩm cũ để import data Bách Hóa Xanh (ảnh local)...", existingCount);
            orderRepository.deleteAll();   // Xóa orders trước (tham chiếu products)
            productRepository.deleteAll(); // Rồi products (tham chiếu shops)
            shopRepository.deleteAll();    // Cuối cùng shops
        }

        // Tạo 3 shop Bách Hóa Xanh ở các quận khác nhau
        Shop shop1 = shopRepository.save(new Shop(
                "Bách Hóa Xanh - Q.1",
                "123 Nguyễn Trãi, Q.1, TP.HCM",
                10.7717, 106.6934,
                "Siêu thị mini"
        ));
        Shop shop2 = shopRepository.save(new Shop(
                "Bách Hóa Xanh - Q.3",
                "456 Lý Thường Kiệt, Q.3, TP.HCM",
                10.7830, 106.6825,
                "Siêu thị mini"
        ));
        Shop shop3 = shopRepository.save(new Shop(
                "Bách Hóa Xanh - Gò Vấp",
                "789 Quang Trung, Gò Vấp, TP.HCM",
                10.8386, 106.6652,
                "Siêu thị mini"
        ));

        Shop[] shops = {shop1, shop2, shop3};
        Random random = new Random(42); // Fixed seed for reproducibility

        // Danh sách file CSV và category tương ứng
        String[][] csvFiles = {
                {"data/Thịt heo.csv", "Thịt heo"},
                {"data/Thịt bò.csv", "Thịt bò"},
                {"data/Thịt gà vịt.csv", "Thịt gà & vịt"},
                {"data/Cá Hải sản.csv", "Cá & Hải sản"},
                {"data/Rau Lá.csv", "Rau lá"},
                {"data/Củ quả.csv", "Củ quả"},
                {"data/Nấm.csv", "Nấm"},
                {"data/Trái cây.csv", "Trái cây"},
                {"data/Trứng gà vịt cút.csv", "Trứng"},
        };

        int totalImported = 0;

        for (String[] csvInfo : csvFiles) {
            String filePath = csvInfo[0];
            String category = csvInfo[1];

            try {
                int count = importCsvFile(filePath, category, shops, random);
                totalImported += count;
                log.info("Imported {} sản phẩm từ category: {}", count, category);
            } catch (Exception e) {
                log.error("Lỗi import file {}: {}", filePath, e.getMessage());
            }
        }

        log.info("=== HOÀN TẤT IMPORT: {} sản phẩm từ Bách Hóa Xanh ===", totalImported);
    }

    /**
     * Import 1 file CSV vào database
     */
    private int importCsvFile(String classpathFile, String category, Shop[] shops, Random random)
            throws IOException {

        ClassPathResource resource = new ClassPathResource(classpathFile);
        if (!resource.exists()) {
            log.warn("File không tồn tại: {}", classpathFile);
            return 0;
        }

        int count = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            // Đọc header để xác định format
            String headerLine = reader.readLine();
            if (headerLine == null) return 0;

            boolean isFormatB = headerLine.contains("Giá gốc") &&
                    headerLine.indexOf("Giá gốc") < headerLine.indexOf("Giá hiện tại");

            // Format A: Name, URL, Image, CurrentPrice, Unit, OriginalPrice, Discount
            // Format B: Name, URL, Image, OriginalPrice, CurrentPrice, Discount, Unit

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    List<String> fields = parseCsvLine(line);
                    if (fields.size() < 5) continue;

                    String name = cleanField(fields.get(0));
                    // fields.get(1) = URL sản phẩm (bỏ qua)
                    // Chuyển URL CDN → local path: /product-images/filename.jpg
                    String rawImageUrl = cleanField(fields.get(2));
                    String imageUrl = convertToLocalImagePath(rawImageUrl);

                    double currentPrice;
                    double originalPrice;
                    String unit;

                    if (isFormatB) {
                        // Format B: index 3=OriginalPrice, 4=CurrentPrice, 5=Discount, 6=Unit
                        originalPrice = parsePrice(fields.get(3));
                        currentPrice = parsePrice(fields.get(4));
                        unit = fields.size() > 6 ? cleanField(fields.get(6)) : "";
                    } else {
                        // Format A: index 3=CurrentPrice, 4=Unit, 5=OriginalPrice, 6=Discount
                        currentPrice = parsePrice(fields.get(3));
                        unit = cleanField(fields.get(4));
                        originalPrice = fields.size() > 5 ? parsePrice(fields.get(5)) : 0;
                    }

                    if (name.isEmpty()) continue;

                    // Nếu không có giá gốc, dùng giá hiện tại làm giá gốc
                    if (originalPrice <= 0) {
                        originalPrice = currentPrice;
                    }
                    // Nếu không có giá hiện tại, dùng giá gốc
                    if (currentPrice <= 0) {
                        currentPrice = originalPrice;
                    }

                    // Skip nếu không có giá
                    if (currentPrice <= 0) continue;

                    // Random shop cho mỗi sản phẩm
                    Shop shop = shops[random.nextInt(shops.length)];

                    // Tạo description tự động
                    String description = name + " tươi ngon từ Bách Hóa Xanh, đảm bảo chất lượng và an toàn vệ sinh thực phẩm.";

                    Product product = new Product();
                    product.setName(name);
                    product.setPrice(currentPrice);
                    product.setOriginalPrice(originalPrice);
                    product.setUnit(unit.isEmpty() ? "phần" : unit);
                    product.setCategory(category);
                    product.setImageUrl(imageUrl);
                    product.setDescription(description);
                    product.setShop(shop);
                    product.setCreatedAt(LocalDateTime.now());

                    productRepository.save(product);
                    count++;

                } catch (Exception e) {
                    log.warn("Lỗi parse dòng CSV: {} - {}", line.substring(0, Math.min(50, line.length())), e.getMessage());
                }
            }
        }

        return count;
    }

    /**
     * Parse một dòng CSV có quoted fields (xử lý dấu phẩy trong quotes)
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]*)\"|([^,]+)");
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                fields.add(matcher.group(1));
            } else {
                fields.add(matcher.group(2).trim());
            }
        }

        return fields;
    }

    /**
     * Parse giá từ string, bỏ dấu phẩy và ký tự không phải số
     */
    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return 0;
        String cleaned = priceStr.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return 0;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Chuyển URL CDN (https://cdnv2.tgdd.vn/.../filename.jpg) → local path (/product-images/filename.jpg)
     * Ảnh đã được tải về thư mục static/product-images/
     */
    private String convertToLocalImagePath(String cdnUrl) {
        if (cdnUrl == null || cdnUrl.isEmpty()) return "";
        // Lấy filename từ URL
        int lastSlash = cdnUrl.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < cdnUrl.length() - 1) {
            String filename = cdnUrl.substring(lastSlash + 1);
            return "/product-images/" + filename;
        }
        return cdnUrl; // fallback giữ nguyên URL gốc
    }

    /**
     * Clean field value - bỏ quotes thừa và trim
     */
    private String cleanField(String field) {
        if (field == null) return "";
        return field.replace("\"", "").trim();
    }

    /**
     * Cào dữ liệu sản phẩm từ Bách Hóa Xanh (giữ lại cho tương lai)
     */
    public int scrapeAll() {
        log.info("Scraping disabled - using CSV import instead");
        return 0;
    }
}
