package com.gomarket.service;

import com.gomarket.model.Product;
import com.gomarket.model.Shop;
import com.gomarket.repository.ProductRepository;
import com.gomarket.repository.ShopRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    public ScraperService(ProductRepository productRepository, ShopRepository shopRepository) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
    }

    /**
     * Cào dữ liệu sản phẩm từ Bách Hóa Xanh
     * URL pattern: https://www.bachhoaxanh.com/{category}
     */
    public int scrapeCategory(String categoryUrl, String categoryName, Shop shop) {
        int count = 0;
        try {
            Document doc = Jsoup.connect(categoryUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();

            // Selector cho Bách Hóa Xanh
            Elements productElements = doc.select(".productList .item, .cate_pro .item");

            for (Element el : productElements) {
                try {
                    String name = el.select(".item_name, .productName").text().trim();
                    String priceText = el.select(".item_price, .productPrice").text()
                            .replaceAll("[^0-9]", "");
                    String imageUrl = el.select("img").attr("src");

                    if (name.isEmpty()) continue;

                    double price = 0;
                    if (!priceText.isEmpty()) {
                        price = Double.parseDouble(priceText);
                    }

                    Product product = new Product(
                            name, price, "phần", categoryName,
                            imageUrl, "", shop
                    );
                    productRepository.save(product);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to parse product: {}", e.getMessage());
                }
            }

            log.info("Scraped {} products from category: {}", count, categoryName);
        } catch (Exception e) {
            log.error("Failed to scrape {}: {}", categoryUrl, e.getMessage());
        }

        return count;
    }

    /**
     * Cào tất cả danh mục chính
     */
    public int scrapeAll() {
        // Tạo shop mặc định
        Shop shop = new Shop(
                "Bách Hóa Xanh - Q.1",
                "123 Nguyễn Trãi, Q.1, TP.HCM",
                10.7717, 106.6934,
                "Siêu thị mini"
        );
        shop = shopRepository.save(shop);

        int total = 0;

        String[][] categories = {
                {"https://www.bachhoaxanh.com/rau-cu", "Rau củ"},
                {"https://www.bachhoaxanh.com/thit-tuoi-song", "Thịt"},
                {"https://www.bachhoaxanh.com/hai-san", "Hải sản"},
                {"https://www.bachhoaxanh.com/trai-cay", "Trái cây"},
                {"https://www.bachhoaxanh.com/gia-vi", "Gia vị"},
        };

        for (String[] cat : categories) {
            total += scrapeCategory(cat[0], cat[1], shop);
        }

        log.info("Total scraped products: {}", total);
        return total;
    }

    /**
     * Tạo dữ liệu mẫu nếu scraping thất bại
     */
    public void seedSampleData() {
        if (productRepository.count() > 0) {
            log.info("Database already has data, skipping seed");
            return;
        }

        // Tạo shops
        Shop shop1 = shopRepository.save(new Shop("Bách Hóa Xanh - Q.1", "123 Nguyễn Trãi, Q.1", 10.7717, 106.6934, "Siêu thị"));
        Shop shop2 = shopRepository.save(new Shop("Bách Hóa Xanh - Q.3", "456 Lý Thường Kiệt, Q.3", 10.7830, 106.6825, "Siêu thị"));
        Shop shop3 = shopRepository.save(new Shop("Chợ Bến Thành", "Chợ Bến Thành, Q.1", 10.7725, 106.6981, "Chợ truyền thống"));

        // Rau củ
        seedProduct("Rau muống", 8000, "bó", "Rau củ", shop1);
        seedProduct("Cà chua", 15000, "kg", "Rau củ", shop1);
        seedProduct("Hành tây", 12000, "kg", "Rau củ", shop2);
        seedProduct("Nấm kim châm", 18000, "gói", "Rau củ", shop1);
        seedProduct("Khoai tây", 20000, "kg", "Rau củ", shop2);

        // Thịt
        seedProduct("Thịt bò Úc", 280000, "kg", "Thịt", shop1);
        seedProduct("Ba chỉ bò Mỹ", 320000, "kg", "Thịt", shop2);
        seedProduct("Thịt heo nạc vai", 95000, "kg", "Thịt", shop1);
        seedProduct("Sườn non heo", 120000, "kg", "Thịt", shop3);
        seedProduct("Gà ta nguyên con", 85000, "con", "Thịt", shop3);

        // Hải sản
        seedProduct("Tôm sú", 180000, "kg", "Hải sản", shop3);
        seedProduct("Mực ống", 150000, "kg", "Hải sản", shop3);
        seedProduct("Cá hồi phi lê", 350000, "kg", "Hải sản", shop1);
        seedProduct("Nghêu", 45000, "kg", "Hải sản", shop3);

        // Trái cây
        seedProduct("Táo Mỹ", 65000, "kg", "Trái cây", shop1);
        seedProduct("Chuối già", 25000, "nải", "Trái cây", shop2);
        seedProduct("Nho xanh Úc", 120000, "kg", "Trái cây", shop1);
        seedProduct("Xoài cát Hòa Lộc", 55000, "kg", "Trái cây", shop3);

        // Gia vị
        seedProduct("Sả", 5000, "bó", "Gia vị", shop3);
        seedProduct("Ớt hiểm", 30000, "kg", "Gia vị", shop3);
        seedProduct("Nước mắm Phú Quốc", 35000, "chai", "Gia vị", shop1);
        seedProduct("Nước cốt dừa", 18000, "hộp", "Gia vị", shop2);
        seedProduct("Lá chanh", 3000, "bó", "Gia vị", shop3);

        log.info("Seeded {} sample products", productRepository.count());
    }

    private void seedProduct(String name, double price, String unit, String category, Shop shop) {
        Product p = new Product(name, price, unit, category, "", name, shop);
        p.setOriginalPrice(price * 1.15); // Giá gốc cao hơn 15%
        productRepository.save(p);
    }
}
