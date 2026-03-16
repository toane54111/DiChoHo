package com.gomarket.config;

import com.gomarket.model.Post;
import com.gomarket.model.Product;
import com.gomarket.model.User;
import com.gomarket.repository.PostRepository;
import com.gomarket.repository.ProductRepository;
import com.gomarket.repository.UserRepository;
import com.gomarket.service.AutocompleteService;
import com.gomarket.service.EmbeddingService;
import com.gomarket.service.ScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(ScraperService scraperService,
                                       UserRepository userRepository,
                                       ProductRepository productRepository,
                                       PostRepository postRepository,
                                       EmbeddingService embeddingService,
                                       AutocompleteService autocompleteService) {
        return args -> {
            log.info("=== KHỞI TẠO DỮ LIỆU MẪU ===");

            seedDemoUsers(userRepository);
            scraperService.seedSampleData();
            seedEmbeddings(productRepository, embeddingService);
            autocompleteService.refreshCache();
            seedCommunityPosts(postRepository, userRepository, embeddingService);

            log.info("=== HOÀN TẤT KHỞI TẠO DỮ LIỆU ===");
        };
    }

    private void seedDemoUsers(UserRepository userRepository) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (!userRepository.existsByPhone("0123456789")) {
            User buyer = new User("Nguyễn Văn Demo", "0123456789", "demo@gomarket.vn",
                    encoder.encode("123456"), "BUYER");
            buyer.setLatitude(10.7769);
            buyer.setLongitude(106.7009);
            userRepository.save(buyer);
            log.info("Đã tạo buyer demo: 0123456789 / 123456");
        }

        if (!userRepository.existsByPhone("0987654321")) {
            User shopper = new User("Trần Thị Shopper", "0987654321", "shopper@gomarket.vn",
                    encoder.encode("123456"), "SHOPPER");
            shopper.setLatitude(10.7800);
            shopper.setLongitude(106.6950);
            shopper.setIsOnline(true);
            shopper.setBio("Đi chợ hộ khu vực Quận 3, Quận 1. Xe máy, giao nhanh trong 30 phút.");
            shopper.setVehicleType("Xe máy");
            shopper.setRating(4.8);
            shopper.setTotalOrders(56);
            userRepository.save(shopper);
            log.info("Đã tạo shopper demo: 0987654321 / 123456");
        }

        // Thêm shoppers demo
        String[][] extraShoppers = {
                {"Lê Văn Tâm", "0912345678", "tam@gomarket.vn", "10.7720", "106.6980",
                        "Chuyên đi chợ Bến Thành, Tân Định. Giao hàng cẩn thận.", "Xe máy", "4.6", "32"},
                {"Phạm Thị Hoa", "0923456789", "hoa@gomarket.vn", "10.7850", "106.7050",
                        "Đi chợ hộ khu Phú Nhuận, Bình Thạnh. Chọn đồ tươi ngon.", "Xe đạp", "4.9", "78"},
                {"Ngô Minh Tuấn", "0934567890", "tuan@gomarket.vn", "10.7650", "106.6900",
                        "Sẵn sàng đi chợ bất kỳ lúc nào. Khu vực Quận 5, 6, 8.", "Xe máy", "4.5", "45"},
        };

        for (String[] s : extraShoppers) {
            if (!userRepository.existsByPhone(s[1])) {
                User shopper = new User(s[0], s[1], s[2], new BCryptPasswordEncoder().encode("123456"), "SHOPPER");
                shopper.setLatitude(Double.parseDouble(s[3]));
                shopper.setLongitude(Double.parseDouble(s[4]));
                shopper.setIsOnline(true);
                shopper.setBio(s[5]);
                shopper.setVehicleType(s[6]);
                shopper.setRating(Double.parseDouble(s[7]));
                shopper.setTotalOrders(Integer.parseInt(s[8]));
                userRepository.save(shopper);
                log.info("Đã tạo shopper: {}", s[0]);
            }
        }
    }

    private void seedCommunityPosts(PostRepository postRepository,
                                     UserRepository userRepository,
                                     EmbeddingService embeddingService) {
        if (postRepository.count() > 0) {
            log.info("Đã có bài đăng cộng đồng, bỏ qua seed.");
            return;
        }

        log.info("Đang seed bài đăng chợ đồng hương...");

        // Lấy user IDs
        List<User> users = userRepository.findAll();
        Long userId1 = users.size() > 0 ? users.get(0).getId() : 1L;
        Long userId2 = users.size() > 1 ? users.get(1).getId() : 1L;
        Long userId3 = users.size() > 2 ? users.get(2).getId() : 1L;

        String[][] posts = {
            // {title, content, category, lat, lng, locationName, userId_index}
            {"Măng le tươi Gia Lai mới hái", "Quê nội gửi lên nhiều quá ăn không hết, nhà em pass lại 3kg măng le tươi, bác nào ở khu vực Tân Bình qua lấy giúp em nhé. Măng non, giòn, ngọt tự nhiên.", "nong_san", "10.8010", "106.6520", "Tân Bình, TP.HCM", "0"},
            {"Bưởi da xanh Bến Tre gom chung", "Góc gom chung: Tối nay vườn dưới Bến Tre cắt bưởi da xanh gửi lên, ai chung mâm cho đỡ tiền cước xe không ạ? Giá gốc 35k/trái, mua từ 5 trái ship free.", "gom_chung", "10.7769", "106.7009", "Quận 3, TP.HCM", "1"},
            {"Hải sản tươi sống từ Vũng Tàu", "Chuyến tàu sáng nay vừa cập bến, em có ít mực ống, tôm sú, cá thu tươi roi rói. Ai cần mua mực, tôm, cá biển inbox em nha, giao tận nhà khu vực Quận 1-3.", "nong_san", "10.7800", "106.6950", "Quận 1, TP.HCM", "2"},
            {"Mắm ruốc Huế xịn đổi rau sạch", "Em có hũ mắm ruốc xịn Huế mới mang vào, đổi lấy mớ rau sạch nhà trồng được không các bác? Mắm ruốc nguyên chất, không phẩm màu.", "rao_vat", "10.7850", "106.7050", "Phú Nhuận, TP.HCM", "0"},
            {"Rau sạch nhà trồng Củ Chi", "Nhà vườn Củ Chi mới thu hoạch rau muống, mồng tơi, rau dền, cải ngọt. Rau hữu cơ không thuốc, tưới nước giếng. Ship nội thành 15k.", "nong_san", "10.9500", "106.5800", "Củ Chi, TP.HCM", "1"},
            {"Thèm lẩu mưa gió ai đi chợ giùm", "Mưa gió lười ra đường quá, thèm nồi lẩu chua cay ghê. Bác nào tiện đường đi CoopMart mua giùm em ít đồ nấu lẩu, em gửi phí 30k ạ.", "rao_vat", "10.7720", "106.6980", "Quận 10, TP.HCM", "2"},
            {"Trứng gà ta quê Bình Phước", "Gà nhà nuôi thả vườn, trứng to đều, lòng đỏ cam đậm. 50k/chục. Ai lấy số lượng nhiều em giảm thêm. Ship COD nội thành.", "nong_san", "10.7769", "106.7009", "Quận 3, TP.HCM", "0"},
            {"Sầu riêng Ri6 Đắk Lắk chín cây", "Mùa sầu riêng tới rồi bà con ơi! Sầu riêng Ri6 chín cây, cơm vàng ươm, ngọt lịm. 120k/kg, mua từ 3kg free ship nội thành.", "dac_san", "10.7800", "106.6950", "Quận 1, TP.HCM", "1"},
            {"Cá lóc đồng mùa nước nổi", "Cá lóc đồng An Giang mùa nước nổi, con nào con nấy mập ú. Làm cá lóc kho tộ, canh chua cá lóc ngon bá cháy. 85k/kg, giao tận nhà.", "nong_san", "10.7650", "106.6900", "Quận 5, TP.HCM", "2"},
            {"Nhà ai có chừng 5kg khoai lang không", "Đang thèm khoai lang nướng mà chợ gần nhà hết rồi. Nhà ai có khoai lang tím Đà Lạt hoặc khoai lang mật dư không ship đến Gò Vấp em nhé.", "rao_vat", "10.8380", "106.6350", "Gò Vấp, TP.HCM", "0"},
            {"Nước mắm Phú Quốc nguyên chất", "Nước mắm Phú Quốc 40 độ đạm, nguyên chất 100%. Chai 500ml giá 75k. Dùng nấu canh, pha chấm đều ngon. Ship nội thành 20k.", "dac_san", "10.7769", "106.7009", "Quận 3, TP.HCM", "1"},
            {"Gom mua thịt bò Úc nhập khẩu", "Có ai muốn gom chung mua thịt bò Úc từ kho lạnh không ạ? Mua sỉ từ 10kg giá chỉ 180k/kg (lẻ 250k). Thịt đông lạnh, chất lượng nhập khẩu.", "gom_chung", "10.7800", "106.6950", "Quận 1, TP.HCM", "2"},
            {"Rau rừng Đà Lạt mới hái sáng nay", "Rau rừng Đà Lạt: rau bồ ngót rừng, đọt su su, rau lang rừng. Hái sáng nay, chiều có mặt ở Sài Gòn. 30k/bó, mua 5 bó giảm 10%.", "nong_san", "10.7850", "106.7050", "Phú Nhuận, TP.HCM", "0"},
            {"Mật ong rừng Tây Nguyên", "Mật ong rừng nguyên chất từ Tây Nguyên, bà con dân tộc khai thác. Chai 500ml giá 200k. Ngâm chanh đào, pha nước uống sáng rất tốt cho sức khỏe.", "dac_san", "10.7720", "106.6980", "Quận 10, TP.HCM", "1"},
            {"Cần người đi chợ giùm khu Thủ Đức", "Em bận họp online cả ngày, cần ai đi chợ giùm mua ít thịt gà, rau cải, trứng, gia vị nấu cơm tối. Gửi phí 25k, chợ gần nhà.", "rao_vat", "10.8490", "106.7710", "Thủ Đức, TP.HCM", "2"},
            {"Cà phê rang xay Buôn Ma Thuột", "Cà phê robusta rang mộc Buôn Ma Thuột, không tẩm ướp hóa chất. Bịch 500g giá 95k. Pha phin đậm đà, thơm lừng cả nhà.", "dac_san", "10.7769", "106.7009", "Quận 3, TP.HCM", "0"},
            {"Gà đi bộ Bình Định mới về", "Gà ta đi bộ Bình Định, thịt dai, da vàng ươm. Con khoảng 1.5-2kg, giá 130k/kg. Làm sẵn hoặc nguyên con tùy bác. Giao nội thành.", "nong_san", "10.7800", "106.6950", "Quận 1, TP.HCM", "1"},
            {"Dừa xiêm Bến Tre tươi nguyên trái", "Dừa xiêm xanh Bến Tre, nước ngọt thanh mát. 15k/trái, mua 10 trái giảm 12k/trái. Ship nội thành, đảm bảo tươi.", "nong_san", "10.7650", "106.6900", "Quận 5, TP.HCM", "2"},
            {"Bánh tráng trộn Tây Ninh", "Bánh tráng trộn sẵn Tây Ninh, đủ gia vị, chỉ cần mở ra trộn là ăn ngay. Bịch lớn 30k. Ship nội thành.", "dac_san", "10.8010", "106.6520", "Tân Bình, TP.HCM", "0"},
            {"Tôm càng xanh Cần Thơ", "Tôm càng xanh nuôi ao Cần Thơ, con to, thịt chắc ngọt. 250k/kg, mua từ 2kg ship free. Hấp bia, nướng muối ớt đều ngon.", "nong_san", "10.7769", "106.7009", "Quận 3, TP.HCM", "1"},
            {"Muối ớt Tây Ninh siêu cay", "Muối ớt xanh Tây Ninh, cay nồng đậm đà, chấm trái cây, gà luộc ngon tuyệt. Hũ 200g giá 25k. Mua 3 hũ giảm 20k/hũ.", "dac_san", "10.7720", "106.6980", "Quận 10, TP.HCM", "2"},
            {"Thanh long ruột đỏ Bình Thuận", "Thanh long ruột đỏ Bình Thuận vừa hái, ngọt lịm, đẹp trái. 25k/kg, thùng 5kg giá 100k. Ship COD nội thành.", "nong_san", "10.7800", "106.6950", "Quận 1, TP.HCM", "0"},
            {"Ai cần đi chợ hộ sáng nay không", "Em rảnh sáng nay, ai cần nhờ đi chợ giùm khu vực Bình Thạnh, Phú Nhuận thì inbox em. Phí ship 20k, em chọn đồ tươi ngon giùm.", "rao_vat", "10.7850", "106.7050", "Bình Thạnh, TP.HCM", "1"},
            {"Ổi lê Đài Loan vườn nhà trồng", "Ổi lê Đài Loan vườn nhà trồng ở Hóc Môn, quả to giòn ngọt. 30k/kg, mua 3kg free ship. Không thuốc trừ sâu, an toàn.", "nong_san", "10.8860", "106.5930", "Hóc Môn, TP.HCM", "2"},
            {"Khô cá dứa một nắng Cà Mau", "Khô cá dứa một nắng Cà Mau, thịt trắng thơm ngọt. Chiên giòn hoặc nướng than đều ngon. 180k/kg, ship COD.", "dac_san", "10.7650", "106.6900", "Quận 5, TP.HCM", "0"},
            {"Đậu phộng rang tỏi ớt Bình Dương", "Đậu phộng rang tỏi ớt nhà làm, giòn rụm, thơm lừng. Bịch 300g giá 35k. Nhậu bia, ăn vặt đều hết sảy.", "dac_san", "10.7769", "106.7009", "Quận 3, TP.HCM", "1"},
            {"Rau má Đà Lạt xay sinh tố", "Rau má tươi Đà Lạt, lá to, xanh mướt. Xay sinh tố mát gan giải nhiệt. 20k/bó lớn, ship nội thành 15k.", "nong_san", "10.7800", "106.6950", "Quận 1, TP.HCM", "2"},
            {"Gom chung mua gạo ST25 Sóc Trăng", "Ai muốn gom chung mua gạo ST25 Sóc Trăng gốc không ạ? Mua sỉ bao 25kg giá 320k (lẻ 18k/kg). Gạo thơm dẻo, nấu cơm ngon nhất thế giới.", "gom_chung", "10.7720", "106.6980", "Quận 10, TP.HCM", "0"},
            {"Chả giò rế Sóc Trăng nhà làm", "Chả giò rế Sóc Trăng nhà tự cuốn, nhân tôm thịt. Chiên giòn vàng rụm. 10 cuốn giá 60k. Đặt trước 1 ngày, giao sáng hôm sau.", "dac_san", "10.7850", "106.7050", "Phú Nhuận, TP.HCM", "1"},
            {"Xoài cát Hòa Lộc chín cây", "Xoài cát Hòa Lộc Tiền Giang chín cây, thơm nức, ngọt lịm. 55k/kg. Mua 3kg ship free khu vực nội thành. Đảm bảo tươi ngon.", "nong_san", "10.7650", "106.6900", "Quận 5, TP.HCM", "2"},
        };

        Long[] userIds = {userId1, userId2, userId3};
        int success = 0;

        for (String[] p : posts) {
            Post post = new Post();
            post.setUserId(userIds[Integer.parseInt(p[6])]);
            post.setTitle(p[0]);
            post.setContent(p[1]);
            post.setCategory(p[2]);
            post.setLatitude(Double.parseDouble(p[3]));
            post.setLongitude(Double.parseDouble(p[4]));
            post.setLocationName(p[5]);

            // RAG: Embed nội dung bài đăng
            try {
                String textToEmbed = p[0] + " " + p[1];
                float[] embedding = embeddingService.embed(textToEmbed);
                post.setEmbedding(embedding);
                success++;
            } catch (Exception e) {
                log.warn("Không thể embed bài đăng: {}", p[0]);
            }

            postRepository.save(post);

            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        log.info("Seed {} bài đăng chợ đồng hương ({} có embedding)", posts.length, success);
    }

    private void seedEmbeddings(ProductRepository productRepository,
                                 EmbeddingService embeddingService) {
        List<Product> products = productRepository.findAll();
        long needEmbedding = products.stream().filter(p -> p.getEmbedding() == null).count();

        if (needEmbedding == 0) {
            log.info("Tất cả sản phẩm đã có embedding, bỏ qua.");
            return;
        }

        log.info("Đang nhúng embedding cho {} sản phẩm...", needEmbedding);
        int success = 0, fail = 0;

        for (Product product : products) {
            if (product.getEmbedding() != null) continue;

            String textToEmbed = product.getName();
            if (product.getCategory() != null) textToEmbed += " - " + product.getCategory();
            if (product.getDescription() != null && !product.getDescription().equals(product.getName()))
                textToEmbed += " - " + product.getDescription();

            float[] vector = embeddingService.embed(textToEmbed);
            if (vector != null) {
                product.setEmbedding(vector);
                productRepository.save(product);
                success++;
            } else {
                fail++;
            }

            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        log.info("Embedding hoàn tất: {} thành công, {} thất bại", success, fail);
    }
}
