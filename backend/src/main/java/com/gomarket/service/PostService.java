package com.gomarket.service;

import com.gomarket.model.*;
import com.gomarket.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gomarket.util.VietnameseUtils;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;

    public PostService(PostRepository postRepository,
                       PostLikeRepository likeRepository,
                       PostCommentRepository commentRepository,
                       UserRepository userRepository,
                       EmbeddingService embeddingService) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public Post createPost(Map<String, Object> body) {
        Post post = new Post();
        post.setUserId(toLong(body.get("userId")));
        post.setTitle((String) body.get("title"));
        post.setContent((String) body.get("content"));
        post.setCategory((String) body.get("category"));
        post.setLatitude(toDouble(body.get("latitude")));
        post.setLongitude(toDouble(body.get("longitude")));
        post.setLocationName((String) body.get("locationName"));
        post.setRegion((String) body.get("region"));
        post.setProvince((String) body.get("province"));

        // RAG: Embed nội dung bài đăng
        try {
            String textToEmbed = post.getTitle() + " " + post.getContent();
            float[] embedding = embeddingService.embed(textToEmbed);
            post.setEmbedding(embedding);
        } catch (Exception e) {
            System.err.println("Không thể embed bài đăng: " + e.getMessage());
        }

        Post saved = postRepository.save(post);

        // Save images
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) body.get("imageUrls");
        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                PostImage img = new PostImage(imageUrls.get(i), i);
                img.setPost(saved);
                saved.getImages().add(img);
            }
            saved = postRepository.save(saved);
        }

        enrichPost(saved, null);
        return saved;
    }

    public List<Post> getFeed(Double lat, Double lng, int page, String category, String region, String province) {
        List<Post> posts;
        boolean hasCategory = category != null && !category.isEmpty();
        boolean hasRegion = region != null && !region.isEmpty();
        boolean hasProvince = province != null && !province.isEmpty();

        if (lat != null && lng != null) {
            posts = postRepository.findNearbyPosts(lat, lng, 20, page * 20);
        } else if (hasCategory && hasRegion && hasProvince) {
            posts = postRepository.findByCategoryAndRegionAndProvince(category, region, province, PageRequest.of(page, 20)).getContent();
        } else if (hasCategory && hasRegion) {
            posts = postRepository.findByCategoryAndRegion(category, region, PageRequest.of(page, 20)).getContent();
        } else if (hasCategory && hasProvince) {
            posts = postRepository.findByCategoryAndProvince(category, province, PageRequest.of(page, 20)).getContent();
        } else if (hasRegion && hasProvince) {
            posts = postRepository.findByIsActiveTrueAndRegionAndProvinceOrderByCreatedAtDesc(region, province, PageRequest.of(page, 20)).getContent();
        } else if (hasRegion) {
            posts = postRepository.findByIsActiveTrueAndRegionOrderByCreatedAtDesc(region, PageRequest.of(page, 20)).getContent();
        } else if (hasProvince) {
            posts = postRepository.findByIsActiveTrueAndProvinceOrderByCreatedAtDesc(province, PageRequest.of(page, 20)).getContent();
        } else if (hasCategory) {
            posts = postRepository.findByIsActiveTrueAndCategoryOrderByCreatedAtDesc(category, PageRequest.of(page, 20)).getContent();
        } else {
            posts = postRepository.findByIsActiveTrueOrderByCreatedAtDesc(PageRequest.of(page, 20)).getContent();
        }

        posts.forEach(post -> enrichPost(post, null));
        return posts;
    }

    // Category display name mapping for search
    private static final Map<String, String> CATEGORY_NAMES = new HashMap<>() {{
        put("nong_san", "Nông sản");
        put("dac_san", "Đặc sản");
        put("rao_vat", "Rao vặt");
        put("gom_chung", "Gom chung");
    }};

    /**
     * RAG Search: Tìm bài đăng tương đồng ngữ nghĩa
     * "mực, tôm" sẽ match "hải sản tươi dưới biển lên"
     * "ca phe" sẽ match "cà phê" (diacritics-aware)
     * "nông sản" sẽ match category nong_san
     */
    public List<Post> searchPosts(String query) {
        // Thử vector search (RAG) trước
        try {
            float[] queryVector = embeddingService.embed(query);
            if (queryVector != null) {
                String vectorStr = Arrays.toString(queryVector);
                List<Post> posts = postRepository.searchByVector(vectorStr, 20);
                if (!posts.isEmpty()) {
                    posts.forEach(post -> enrichPost(post, null));
                    return posts;
                }
            }
        } catch (Exception e) {
            System.err.println("Vector search thất bại, fallback text search: " + e.getMessage());
        }

        // Fallback: enhanced text search with diacritics + category matching
        return searchByTextEnhanced(query);
    }

    /**
     * Text search nâng cao: hỗ trợ không dấu + tìm theo category
     * "ca phe" → match "cà phê"
     * "nong san" → match category nông sản
     */
    private List<Post> searchByTextEnhanced(String query) {
        // 1. Thử search bằng text gốc trước
        List<Post> results = postRepository.searchByText(query);

        // 2. Tìm theo category name (e.g. "nông sản" → category "nong_san")
        String matchedCategory = findCategoryByQuery(query);
        if (matchedCategory != null) {
            List<Post> categoryPosts = postRepository
                    .findByIsActiveTrueAndCategoryOrderByCreatedAtDesc(matchedCategory,
                            org.springframework.data.domain.PageRequest.of(0, 20))
                    .getContent();
            // Merge, avoid duplicates
            java.util.Set<Long> existingIds = results.stream().map(Post::getId).collect(Collectors.toSet());
            for (Post p : categoryPosts) {
                if (!existingIds.contains(p.getId())) {
                    results.add(p);
                }
            }
        }

        // 3. Nếu kết quả ít, thử search không dấu (diacritics-free)
        if (results.size() < 5) {
            String normalized = VietnameseUtils.removeDiacritics(query);
            if (!normalized.equals(query.toLowerCase().trim())) {
                // Query có dấu, đã search rồi, skip
            } else {
                // Query không dấu → search trên tất cả posts bằng Java filter
                List<Post> allActive = postRepository.findByIsActiveTrueOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(0, 200)).getContent();
                java.util.Set<Long> existingIds = results.stream().map(Post::getId).collect(Collectors.toSet());
                for (Post p : allActive) {
                    if (existingIds.contains(p.getId())) continue;
                    String titleNorm = VietnameseUtils.removeDiacritics(p.getTitle());
                    String contentNorm = VietnameseUtils.removeDiacritics(p.getContent());
                    String locationNorm = VietnameseUtils.removeDiacritics(p.getLocationName());
                    String provinceNorm = VietnameseUtils.removeDiacritics(p.getProvince());
                    if (titleNorm.contains(normalized) || contentNorm.contains(normalized)
                            || locationNorm.contains(normalized) || provinceNorm.contains(normalized)) {
                        results.add(p);
                    }
                    if (results.size() >= 20) break;
                }
            }
        }

        results.forEach(post -> enrichPost(post, null));
        return results;
    }

    /** Map query text to category key (supports diacritics-free matching) */
    private String findCategoryByQuery(String query) {
        String queryNorm = VietnameseUtils.removeDiacritics(query);
        for (Map.Entry<String, String> entry : CATEGORY_NAMES.entrySet()) {
            String categoryNorm = VietnameseUtils.removeDiacritics(entry.getValue());
            String categoryKey = entry.getKey().replace("_", " ");
            if (queryNorm.contains(categoryNorm) || queryNorm.contains(categoryKey)
                    || categoryNorm.contains(queryNorm)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Post getPost(Long id, Long viewerUserId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng"));
        enrichPost(post, viewerUserId);
        return post;
    }

    public List<Post> getUserPosts(Long userId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        posts.forEach(post -> enrichPost(post, null));
        return posts;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        var existing = likeRepository.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            likeRepository.save(new PostLike(postId, userId));
            liked = true;
        }
        int count = likeRepository.countByPostId(postId);
        return Map.of("liked", liked, "likeCount", count);
    }

    @Transactional
    public PostComment addComment(Long postId, Long userId, String content) {
        PostComment comment = new PostComment(postId, userId, content);
        comment = commentRepository.save(comment);
        enrichComment(comment);
        return comment;
    }

    public List<PostComment> getComments(Long postId) {
        List<PostComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        comments.forEach(this::enrichComment);
        return comments;
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng"));
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài đăng này");
        }
        post.setIsActive(false);
        postRepository.save(post);
    }

    private void enrichPost(Post post, Long viewerUserId) {
        userRepository.findById(post.getUserId()).ifPresent(user -> {
            post.setAuthorName(user.getFullName());
            post.setAuthorAvatar(user.getAvatarUrl());
            post.setAuthorPhone(user.getPhone());
        });
        post.setLikeCount(likeRepository.countByPostId(post.getId()));
        post.setCommentCount(commentRepository.countByPostId(post.getId()));
        if (viewerUserId != null) {
            post.setIsLikedByUser(likeRepository.existsByPostIdAndUserId(post.getId(), viewerUserId));
        }
    }

    private void enrichComment(PostComment comment) {
        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            comment.setAuthorName(user.getFullName());
            comment.setAuthorAvatar(user.getAvatarUrl());
        });
    }

    @Transactional
    public void seedCommunityPosts() {
        // Xóa bài cũ không có province và seed lại
        long countWithProvince = postRepository.findByIsActiveTrueOrderByCreatedAtDesc(
                PageRequest.of(0, 1)).stream()
                .filter(p -> p.getProvince() != null && !p.getProvince().isEmpty())
                .count();
        if (postRepository.count() > 10 && countWithProvince > 0) return;

        // Xóa bài seed cũ (soft delete)
        postRepository.findAll().forEach(p -> {
            if (p.getProvince() == null || p.getProvince().isEmpty()) {
                p.setIsActive(false);
                postRepository.save(p);
            }
        });

        Long userId = userRepository.findAll().stream()
                .map(User::getId).findFirst().orElse(1L);

        // {title, content, category, region, province, locationName}
        Object[][] seeds = {
            // ═══ MIỀN BẮC ═══
            {"Bưởi Diễn đặc sản Hà Nội", "Bưởi Diễn chính gốc, vỏ mỏng, múi mọng nước, vị ngọt thanh. Mùa Tết bán sỉ lẻ, 50k/quả.", "nong_san", "MIEN_BAC", "Hà Nội", "Phúc Diễn, Bắc Từ Liêm"},
            {"Bánh cốm Hàng Than", "Bánh cốm truyền thống phố Hàng Than, nhân đậu xanh dừa. Đặt trước 1 ngày.", "dac_san", "MIEN_BAC", "Hà Nội", "Hàng Than, Ba Đình"},
            {"Ai cần mua đồ khô Hà Nội?", "Mình về quê tuần sau, ai cần mua miến dong, mộc nhĩ, nấm hương Bắc thì nhờ mình mua giúp.", "rao_vat", "MIEN_BAC", "Hà Nội", "Hoàn Kiếm"},
            {"Chả mực Hạ Long chính gốc", "Chả mực giã tay truyền thống, thịt mực tươi 100%, dai giòn thơm. Ship đông lạnh giữ nguyên vị.", "dac_san", "MIEN_BAC", "Quảng Ninh", "Hạ Long"},
            {"Vải thiều Bắc Giang xuất khẩu", "Vải thiều loại 1 xuất khẩu, quả to đều, ngọt thơm. Đặt sớm có giá tốt. Ship đông lạnh toàn quốc.", "nong_san", "MIEN_BAC", "Bắc Giang", "Lục Ngạn"},
            {"Gom đơn vải thiều về Sài Gòn", "Mùa vải thiều sắp tới, gom đơn ship từ Bắc Giang vào Sài Gòn. Giá tại vườn + phí ship chia đều.", "gom_chung", "MIEN_BAC", "Bắc Giang", "Lục Ngạn"},
            {"Nhãn lồng Hưng Yên chính vụ", "Nhãn lồng đầu mùa, cùi dày, hạt nhỏ, ngọt lịm. Đóng thùng xốp ship xa. Giá 45k/kg.", "nong_san", "MIEN_BAC", "Hưng Yên", "Khoái Châu"},
            {"Gạo nếp cái hoa vàng Thái Bình", "Gạo nếp thơm dẻo, nấu xôi cực ngon. Nhà trồng 100% organic, không thuốc. 35k/kg.", "nong_san", "MIEN_BAC", "Thái Bình", "Kiến Xương"},
            {"Phở bò Nam Định ship tận nơi", "Bộ nguyên liệu phở bò Nam Định chính gốc: bánh phở tươi + nước dùng + thịt bò. Nấu tại nhà 15 phút.", "dac_san", "MIEN_BAC", "Nam Định", "TP Nam Định"},
            {"Hải sản tươi sống Hải Phòng", "Tôm sú, cua gạch, ghẹ, ngao hoa đánh bắt sáng nay. Ship đông lạnh giữ tươi, giá chợ đầu mối.", "nong_san", "MIEN_BAC", "Hải Phòng", "Đồ Sơn"},
            {"Miến dong Bắc Kạn chính gốc", "Miến dong sợi dai, nấu không nát. Làm thủ công truyền thống. 60k/kg, mua 5kg free ship.", "dac_san", "MIEN_BAC", "Bắc Kạn", "Na Rì"},
            {"Thịt trâu gác bếp Sơn La", "Thịt trâu hun khói gác bếp, đặc sản Tây Bắc. Nhậu lai rai hoặc làm quà tuyệt vời. 350k/kg.", "dac_san", "MIEN_BAC", "Sơn La", "Mộc Châu"},
            {"Chè Thái Nguyên Tân Cương", "Chè búp Tân Cương loại 1, hương thơm đặc trưng. Hộp 200g giá 100k. Gom đơn giá sỉ.", "nong_san", "MIEN_BAC", "Thái Nguyên", "Tân Cương"},
            {"Cam Cao Phong mùa mới thu hoạch", "Cam Cao Phong Hòa Bình, vỏ mỏng, ngọt lịm. Hàng tuyển, đóng thùng 10kg ship toàn quốc.", "nong_san", "MIEN_BAC", "Hòa Bình", "Cao Phong"},
            {"Bánh đậu xanh Hải Dương", "Bánh đậu xanh Rồng Vàng chính hãng. Hộp 10 cái 40k. Mua làm quà biếu Tết.", "dac_san", "MIEN_BAC", "Hải Dương", "TP Hải Dương"},

            // ═══ MIỀN TRUNG ═══
            {"Cam Vinh ngon ngọt mùa mới", "Nhà em có 200kg cam Vinh, quả to, ngọt thanh, không hạt. Ship toàn quốc, mua nhiều giá sỉ.", "nong_san", "MIEN_TRUNG", "Nghệ An", "Quỳ Hợp"},
            {"Mắm ruốc Huế gia truyền", "Mắm ruốc Huế làm thủ công, ủ 6 tháng, thơm đậm đà. Ăn với bún bò, cơm hến tuyệt vời. 40k/hũ 500g.", "dac_san", "MIEN_TRUNG", "Thừa Thiên Huế", "TP Huế"},
            {"Tìm người ship bún bò Huế", "Mình ở Sài Gòn muốn nhờ ai ở Huế mua giúp bộ nguyên liệu bún bò (mắm ruốc, sả, ớt...) ship vào.", "rao_vat", "MIEN_TRUNG", "Thừa Thiên Huế", "TP Huế"},
            {"Bánh tráng dẻo Quảng Nam", "Bánh tráng dẻo cuốn thịt heo, bánh tráng nướng chấm mắm nêm. Combo 1kg bánh tráng các loại 80k.", "dac_san", "MIEN_TRUNG", "Quảng Nam", "Đại Lộc"},
            {"Mì Quảng Phú Chiêm chính gốc", "Bộ nguyên liệu mì Quảng: mì tươi, nước lèo, đậu phộng. Ship đông lạnh. 80k/phần 4 người.", "dac_san", "MIEN_TRUNG", "Quảng Nam", "Điện Bàn"},
            {"Hải sản tươi Đà Nẵng", "Tôm hùm, cua Hoàng Đế, mực ống tươi sống từ biển Sơn Trà. Giao hàng trong ngày. Giá chợ đầu mối.", "nong_san", "MIEN_TRUNG", "Đà Nẵng", "Sơn Trà"},
            {"Nước mắm Phan Thiết loại 1", "Nước mắm nhĩ 40 độ đạm, thơm ngon tự nhiên. Chai 500ml giá 50k. Thùng 12 chai giảm 20%.", "dac_san", "MIEN_TRUNG", "Bình Thuận", "Phan Thiết"},
            {"Thanh long ruột đỏ Bình Thuận", "Thanh long ruột đỏ, quả đẹp đều, ngọt mát. Hàng vườn nhà, không thuốc. 25k/kg, 10kg free ship.", "nong_san", "MIEN_TRUNG", "Bình Thuận", "Hàm Thuận Nam"},
            {"Bơ sáp Đắk Lắk ngon béo", "Bơ sáp 034, quả to 300-500g, dẻo béo ngậy. Ship nhanh để bơ chín đều. Giá 55k/kg.", "nong_san", "MIEN_TRUNG", "Đắk Lắk", "Buôn Ma Thuột"},
            {"Cà phê Robusta Đắk Lắk", "Cà phê Robusta rang xay, đậm đà hương vị Tây Nguyên. 500g giá 80k. Mua 2kg giảm 15%.", "dac_san", "MIEN_TRUNG", "Đắk Lắk", "Cư M'gar"},
            {"Sầu riêng Ri6 Đắk Nông", "Sầu riêng Ri6 chín cây, cơm vàng, hạt lép, ngọt sắc. Đóng thùng cẩn thận. Giá 85k/kg.", "nong_san", "MIEN_TRUNG", "Đắk Nông", "Đắk Mil"},
            {"Măng le tươi Gia Lai", "Nhà em có 50kg măng le tươi, ai cần inbox ship tận nơi. Măng rừng tự nhiên, giòn ngon.", "nong_san", "MIEN_TRUNG", "Gia Lai", "Mang Yang"},
            {"Cà phê Arabica Đà Lạt rang mộc", "Cà phê Arabica trồng 1400m, rang mộc 100%, hương chocolate nhẹ. 500g bột/hạt giá 120k.", "dac_san", "MIEN_TRUNG", "Lâm Đồng", "Đà Lạt"},
            {"Khoai lang Đà Lạt organic", "Khoai lang Nhật Đà Lạt, trồng organic, ruột vàng bở ngọt. Thùng 5kg giá 80k. Ship toàn quốc.", "nong_san", "MIEN_TRUNG", "Lâm Đồng", "Đà Lạt"},
            {"Dâu tây Đà Lạt tươi hái sáng", "Dâu tây chín đỏ, ngọt thơm, hái sáng ship chiều. Hộp 500g giá 70k. Mua làm sinh tố, kem.", "nong_san", "MIEN_TRUNG", "Lâm Đồng", "Đà Lạt"},
            {"Nho Ninh Thuận ngọt giòn", "Nho xanh, nho đỏ Ninh Thuận, quả mọng tự nhiên. 35k/kg, thùng 5kg free ship.", "nong_san", "MIEN_TRUNG", "Ninh Thuận", "Ninh Phước"},
            {"Yến sào Khánh Hòa chính gốc", "Yến sào Nha Trang, tổ yến thô đã làm sạch. 100g giá 1.2 triệu. Hàng chất lượng có giấy tờ.", "dac_san", "MIEN_TRUNG", "Khánh Hòa", "Nha Trang"},
            {"Nem chua Thanh Hóa", "Nem chua truyền thống Thanh Hóa, chua cay vừa miệng. 100 cái giá 150k. Ship đông lạnh.", "dac_san", "MIEN_TRUNG", "Thanh Hóa", "TP Thanh Hóa"},

            // ═══ MIỀN NAM ═══
            {"Xoài cát Hòa Lộc Tiền Giang", "Xoài cát Hòa Lộc chín cây, thơm ngọt béo, không xơ. Đóng hộp quà tặng sang trọng. 80k/kg.", "nong_san", "MIEN_NAM", "Tiền Giang", "Cái Bè"},
            {"Dừa xiêm Bến Tre tươi", "Dừa xiêm xanh, nước ngọt mát, cơm dừa dẻo thơm. Ship nguyên quả hoặc đã gọt. 15k/quả.", "nong_san", "MIEN_NAM", "Bến Tre", "Châu Thành"},
            {"Kẹo dừa Bến Tre thủ công", "Kẹo dừa sữa, kẹo dừa đậu phộng, đủ vị. Hộp 500g giá 40k. Đặc sản miền Tây.", "dac_san", "MIEN_NAM", "Bến Tre", "TP Bến Tre"},
            {"Chôm chôm Java Vĩnh Long", "Chôm chôm Java quả to, cùi dày tách hạt, ngọt lịm. Hàng vườn tươi ngon. 30k/kg.", "nong_san", "MIEN_NAM", "Vĩnh Long", "Bình Minh"},
            {"Cá khô một nắng Cà Mau", "Cá lóc, cá sặc một nắng muối ớt, đặc sản miền Tây. Chiên giòn hoặc nướng đều ngon. 150k/kg.", "dac_san", "MIEN_NAM", "Cà Mau", "Năm Căn"},
            {"Tôm khô Cà Mau loại 1", "Tôm khô size lớn, màu đỏ tự nhiên, không tẩm hóa chất. Ăn Tết, nấu súp đều ngon. 500k/kg.", "dac_san", "MIEN_NAM", "Cà Mau", "Ngọc Hiển"},
            {"Bánh pía Sóc Trăng", "Bánh pía đậu xanh sầu riêng, thơm lừng béo ngậy. Hộp 4 cái 60k. Đặc sản miền Tây chính gốc.", "dac_san", "MIEN_NAM", "Sóc Trăng", "TP Sóc Trăng"},
            {"Gom chung mua gạo ST25 giá sỉ", "Ai TP.HCM muốn mua gạo ST25 Sóc Trăng giá gốc thì gom chung. Đủ 100kg mình order luôn, 22k/kg.", "gom_chung", "MIEN_NAM", "Sóc Trăng", "Mỹ Xuyên"},
            {"Mắm cá linh mùa nước nổi", "Mắm cá linh ủ truyền thống, thơm ngon đậm đà. Kho với cà tím, ăn cơm trắng là nhất. 55k/hũ.", "dac_san", "MIEN_NAM", "An Giang", "Châu Đốc"},
            {"Mắm Châu Đốc các loại", "Mắm thái, mắm cá linh, mắm cá sặc. Đặc sản An Giang chính gốc. Combo 3 hũ 120k.", "dac_san", "MIEN_NAM", "An Giang", "Châu Đốc"},
            {"Gom đơn mua trái cây miền Tây", "Ai ở Sài Gòn muốn mua trái cây miền Tây (măng cụt, chôm chôm, sầu riêng) inbox gom đơn ship tuần này.", "gom_chung", "MIEN_NAM", "TP. Hồ Chí Minh", "Quận 1"},
            {"Bán mít Thái sấy dẻo homemade", "Mít Thái sấy dẻo tự làm, không đường không chất bảo quản. Túi 200g giá 45k. Ăn vặt healthy!", "dac_san", "MIEN_NAM", "Bình Dương", "Thuận An"},
            {"Gom đơn hải sản Vũng Tàu", "Mình đi Vũng Tàu cuối tuần, ai cần mua tôm, mực, cá tươi thì inbox. Gom đủ 10 đơn mình chạy.", "gom_chung", "MIEN_NAM", "Bà Rịa - Vũng Tàu", "TP Vũng Tàu"},
            {"Bưởi da xanh Đồng Nai", "Bưởi da xanh ruột hồng, ngọt mát, không hạt. Nhà vườn trực tiếp, 45k/quả.", "nong_san", "MIEN_NAM", "Đồng Nai", "Tân Phú"},
            {"Sầu riêng Monthong Long Khánh", "Sầu riêng Monthong chín cây, cơm vàng dày, hạt lép. Giá tại vườn 75k/kg, ship tận nơi.", "nong_san", "MIEN_NAM", "Đồng Nai", "Long Khánh"},
            {"Hủ tiếu Sa Đéc sấy khô", "Hủ tiếu Sa Đéc sợi trong, dai giòn. 500g giá 25k. Nấu nước lèo, xào đều ngon.", "dac_san", "MIEN_NAM", "Đồng Tháp", "Sa Đéc"},
            {"Khô cá lóc Đồng Tháp", "Khô cá lóc phơi nắng tự nhiên, thịt dày, chiên giòn béo ngậy. 200k/kg.", "dac_san", "MIEN_NAM", "Đồng Tháp", "Tháp Mười"},
            {"Bánh tráng trộn Tây Ninh", "Bánh tráng trộn đầy đủ topping: khô bò, trứng cút, sa tế. Bịch 50k. Ship TPHCM trong ngày.", "dac_san", "MIEN_NAM", "Tây Ninh", "Trảng Bàng"},
            {"Măng cụt Lái Thiêu mùa mới", "Măng cụt Lái Thiêu vỏ đỏ, cùi trắng ngọt thanh. Hàng chợ đầu mối, 40k/kg.", "nong_san", "MIEN_NAM", "Bình Dương", "Thuận An"},
            {"Trái cây mix miền Tây giá sỉ", "Combo trái cây miền Tây: xoài, chôm chôm, măng cụt, nhãn. Thùng 10kg giá 180k. Free ship TPHCM.", "gom_chung", "MIEN_NAM", "Cần Thơ", "Cái Răng"},
        };

        for (Object[] s : seeds) {
            Post post = new Post();
            post.setUserId(userId);
            post.setTitle((String) s[0]);
            post.setContent((String) s[1]);
            post.setCategory((String) s[2]);
            post.setRegion((String) s[3]);
            post.setProvince((String) s[4]);
            post.setLocationName((String) s[5]);
            post.setIsActive(true);
            postRepository.save(post);
        }
        System.out.println("Seeded " + seeds.length + " community posts with provinces");
    }

    /** Danh sách tỉnh thành theo vùng miền (63 tỉnh thành Việt Nam) */
    public static Map<String, List<String>> getProvincesByRegion() {
        Map<String, List<String>> map = new java.util.LinkedHashMap<>();
        map.put("MIEN_BAC", Arrays.asList(
            "Hà Nội", "Hải Phòng", "Quảng Ninh", "Bắc Giang", "Bắc Kạn", "Bắc Ninh",
            "Cao Bằng", "Điện Biên", "Hà Giang", "Hà Nam", "Hải Dương", "Hòa Bình",
            "Hưng Yên", "Lai Châu", "Lạng Sơn", "Lào Cai", "Nam Định", "Ninh Bình",
            "Phú Thọ", "Sơn La", "Thái Bình", "Thái Nguyên", "Tuyên Quang", "Vĩnh Phúc", "Yên Bái"
        ));
        map.put("MIEN_TRUNG", Arrays.asList(
            "Thanh Hóa", "Nghệ An", "Hà Tĩnh", "Quảng Bình", "Quảng Trị",
            "Thừa Thiên Huế", "Đà Nẵng", "Quảng Nam", "Quảng Ngãi", "Bình Định",
            "Phú Yên", "Khánh Hòa", "Ninh Thuận", "Bình Thuận",
            "Kon Tum", "Gia Lai", "Đắk Lắk", "Đắk Nông", "Lâm Đồng"
        ));
        map.put("MIEN_NAM", Arrays.asList(
            "TP. Hồ Chí Minh", "Bà Rịa - Vũng Tàu", "Bình Dương", "Bình Phước",
            "Đồng Nai", "Tây Ninh", "Long An", "Tiền Giang", "Bến Tre", "Trà Vinh",
            "Vĩnh Long", "Đồng Tháp", "An Giang", "Kiên Giang", "Cần Thơ",
            "Hậu Giang", "Sóc Trăng", "Bạc Liêu", "Cà Mau"
        ));
        return map;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.parseDouble(val.toString());
    }
}
