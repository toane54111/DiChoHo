package com.gomarket.config;

import com.gomarket.model.Product;
import com.gomarket.model.User;
import com.gomarket.repository.ProductRepository;
import com.gomarket.repository.UserRepository;
import com.gomarket.service.EmbeddingService;
import com.gomarket.service.ScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

/**
 * DataInitializer - Khởi tạo dữ liệu mẫu khi ứng dụng chạy lần đầu
 *
 * Flow:
 * 1. Seed user demo (buyer + shopper)
 * 2. Seed shop + product data
 * 3. Nhúng tất cả sản phẩm thành vector embedding (RAG)
 *    → Gọi Ollama nomic-embed-text (local) cho từng sản phẩm
 *    → Lưu vector 768 chiều vào cột embedding trong PostgreSQL (pgvector)
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(ScraperService scraperService,
                                       UserRepository userRepository,
                                       ProductRepository productRepository,
                                       EmbeddingService embeddingService) {
        return args -> {
            log.info("=== KHỞI TẠO DỮ LIỆU MẪU ===");

            // 1. Tạo user demo nếu chưa có
            seedDemoUsers(userRepository);

            // 2. Seed sản phẩm mẫu (shops + products)
            scraperService.seedSampleData();

            // 3. Nhúng embedding cho tất cả sản phẩm chưa có vector
            seedEmbeddings(productRepository, embeddingService);

            log.info("=== HOÀN TẤT KHỞI TẠO DỮ LIỆU ===");
        };
    }

    private void seedDemoUsers(UserRepository userRepository) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Buyer demo
        if (!userRepository.existsByPhone("0123456789")) {
            User buyer = new User(
                    "Nguyễn Văn Demo",
                    "0123456789",
                    "demo@gomarket.vn",
                    encoder.encode("123456"),
                    "BUYER"
            );
            userRepository.save(buyer);
            log.info("Đã tạo tài khoản buyer demo: 0123456789 / 123456");
        }

        // Shopper demo
        if (!userRepository.existsByPhone("0987654321")) {
            User shopper = new User(
                    "Trần Thị Shopper",
                    "0987654321",
                    "shopper@gomarket.vn",
                    encoder.encode("123456"),
                    "SHOPPER"
            );
            userRepository.save(shopper);
            log.info("Đã tạo tài khoản shopper demo: 0987654321 / 123456");
        }
    }

    /**
     * Nhúng tất cả sản phẩm chưa có embedding vector
     * Gọi Ollama nomic-embed-text (local) cho mỗi sản phẩm
     *
     * Text nhúng = "tên sản phẩm + category + description"
     * → Giúp vector chứa đủ ngữ nghĩa để match với nguyên liệu
     */
    private void seedEmbeddings(ProductRepository productRepository,
                                 EmbeddingService embeddingService) {
        List<Product> products = productRepository.findAll();

        // Đếm sản phẩm chưa có embedding
        long needEmbedding = products.stream()
                .filter(p -> p.getEmbedding() == null)
                .count();

        if (needEmbedding == 0) {
            log.info("Tất cả sản phẩm đã có embedding, bỏ qua.");
            return;
        }

        log.info("Đang nhúng embedding cho {} sản phẩm...", needEmbedding);

        int success = 0;
        int fail = 0;

        for (Product product : products) {
            if (product.getEmbedding() != null) {
                continue; // Đã có embedding rồi, bỏ qua
            }

            // Tạo text tổng hợp để nhúng: tên + danh mục + mô tả
            String textToEmbed = buildEmbeddingText(product);

            float[] vector = embeddingService.embed(textToEmbed);
            if (vector != null) {
                product.setEmbedding(vector);
                productRepository.save(product);
                success++;
                log.info("  ✓ Embedded [{}]: '{}' → vector[{}]",
                        product.getId(), product.getName(), vector.length);
            } else {
                fail++;
                log.warn("  ✗ Failed to embed: '{}'", product.getName());
            }

            // Delay nhẹ để tránh rate limit API
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Embedding hoàn tất: {} thành công, {} thất bại", success, fail);
    }

    /**
     * Tạo text tổng hợp để nhúng
     * VD: "Thịt bò Úc - Thịt - thực phẩm tươi sống bò nhập khẩu"
     */
    private String buildEmbeddingText(Product product) {
        StringBuilder sb = new StringBuilder();
        sb.append(product.getName());

        if (product.getCategory() != null && !product.getCategory().isEmpty()) {
            sb.append(" - ").append(product.getCategory());
        }
        if (product.getDescription() != null && !product.getDescription().isEmpty()
                && !product.getDescription().equals(product.getName())) {
            sb.append(" - ").append(product.getDescription());
        }

        return sb.toString();
    }
}
