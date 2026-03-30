package com.gomarket.service;

import com.gomarket.model.*;
import com.gomarket.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CookbookService {

    private final CookbookRecipeRepository recipeRepository;
    private final CookbookLikeRepository likeRepository;
    private final CookbookCommentRepository commentRepository;
    private final UserRepository userRepository;

    public CookbookService(CookbookRecipeRepository recipeRepository,
                           CookbookLikeRepository likeRepository,
                           CookbookCommentRepository commentRepository,
                           UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CookbookRecipe createRecipe(Map<String, Object> body) {
        CookbookRecipe recipe = new CookbookRecipe();
        recipe.setUserId(toLong(body.get("userId")));
        recipe.setTitle((String) body.get("title"));
        recipe.setDescription((String) body.get("description"));
        recipe.setIngredientsJson((String) body.get("ingredientsJson"));
        recipe.setStepsJson((String) body.get("stepsJson"));
        recipe.setImageUrl((String) body.get("imageUrl"));
        recipe.setTotalCost(toDouble(body.get("totalCost")));
        recipe.setIsSystemRecipe(Boolean.TRUE.equals(body.get("isSystemRecipe")));

        recipe = recipeRepository.save(recipe);
        enrichRecipe(recipe, null);
        return recipe;
    }

    public List<CookbookRecipe> getSuggestions(int page) {
        List<CookbookRecipe> recipes = recipeRepository
                .findByIsSystemRecipeTrueOrderByCreatedAtDesc(PageRequest.of(page, 20))
                .getContent();
        recipes.forEach(r -> enrichRecipe(r, null));
        return recipes;
    }

    public List<CookbookRecipe> getCommunityRecipes(int page, Long viewerUserId) {
        List<CookbookRecipe> recipes = recipeRepository
                .findCommunityRecipesByPopularity(PageRequest.of(page, 20))
                .getContent();
        recipes.forEach(r -> enrichRecipe(r, viewerUserId));
        return recipes;
    }

    public List<CookbookRecipe> getPersonalRecipes(Long userId) {
        // Merge user's own recipes + liked recipes, deduplicate
        List<CookbookRecipe> own = recipeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<CookbookRecipe> liked = recipeRepository.findLikedByUser(userId);

        Set<Long> seenIds = new HashSet<>();
        List<CookbookRecipe> merged = new ArrayList<>();
        for (CookbookRecipe r : own) {
            if (seenIds.add(r.getId())) merged.add(r);
        }
        for (CookbookRecipe r : liked) {
            if (seenIds.add(r.getId())) merged.add(r);
        }

        merged.forEach(r -> enrichRecipe(r, userId));
        return merged;
    }

    public CookbookRecipe getRecipe(Long id, Long viewerUserId) {
        CookbookRecipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công thức"));
        enrichRecipe(recipe, viewerUserId);
        return recipe;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long recipeId, Long userId) {
        var existing = likeRepository.findByRecipeIdAndUserId(recipeId, userId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            likeRepository.save(new CookbookLike(recipeId, userId));
            liked = true;
        }
        int count = likeRepository.countByRecipeId(recipeId);
        return Map.of("liked", liked, "likeCount", count);
    }

    @Transactional
    public CookbookComment addComment(Long recipeId, Long userId, String content) {
        CookbookComment comment = new CookbookComment(recipeId, userId, content);
        comment = commentRepository.save(comment);
        enrichComment(comment);
        return comment;
    }

    public List<CookbookComment> getComments(Long recipeId) {
        List<CookbookComment> comments = commentRepository.findByRecipeIdOrderByCreatedAtAsc(recipeId);
        comments.forEach(this::enrichComment);
        return comments;
    }

    @Transactional
    public void deleteRecipe(Long recipeId, Long userId) {
        CookbookRecipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công thức"));
        if (!recipe.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa công thức này");
        }
        recipeRepository.delete(recipe);
    }

    /**
     * Force re-seed: xóa tất cả system + community recipes rồi tạo lại
     */
    @Transactional
    public void forceReseedRecipes() {
        recipeRepository.deleteAll();
        doSeedRecipes();
    }

    @Transactional
    public void seedSystemRecipes() {
        long count = recipeRepository.findByIsSystemRecipeTrueOrderByCreatedAtDesc(PageRequest.of(0, 1))
                .getTotalElements();
        if (count >= 20) return;
        // Xóa cũ nếu chỉ có 5 món cũ
        if (count > 0 && count < 20) {
            recipeRepository.findAll().stream()
                    .filter(CookbookRecipe::getIsSystemRecipe)
                    .forEach(recipeRepository::delete);
        }
        doSeedRecipes();
    }

    private void doSeedRecipes() {
        Long userId = userRepository.findAll().stream()
                .map(User::getId).findFirst().orElse(1L);

        // ═══════════════════════════════════════════════
        // 20 MÓN GỢI Ý HỆ THỐNG (System Recipes)
        // ═══════════════════════════════════════════════

        createSystemRecipe("Sườn Xào Chua Ngọt", "Sườn heo non xào sốt chua ngọt đậm đà, thơm lừng", "/uploads/seed/suon_xao_chua_ngot.jpg",
                "[{\"name\":\"Sườn heo non\",\"quantity\":\"500g\",\"estimated_price\":85000}," +
                "{\"name\":\"Hành tây\",\"quantity\":\"1 củ\",\"estimated_price\":8000}," +
                "{\"name\":\"Ớt chuông\",\"quantity\":\"1 trái\",\"estimated_price\":12000}," +
                "{\"name\":\"Tỏi\",\"quantity\":\"3 tép\",\"estimated_price\":3000}," +
                "{\"name\":\"Cà chua\",\"quantity\":\"2 trái\",\"estimated_price\":10000}," +
                "{\"name\":\"Dấm\",\"quantity\":\"2 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Đường\",\"quantity\":\"3 muỗng\",\"estimated_price\":3000}," +
                "{\"name\":\"Nước mắm\",\"quantity\":\"2 muỗng\",\"estimated_price\":5000}]",
                "[\"Sườn chặt miếng vừa ăn, ướp muối + tiêu + tỏi băm 15 phút\"," +
                "\"Chiên sườn vàng giòn, vớt ra để ráo dầu\"," +
                "\"Phi tỏi thơm, cho cà chua xào nhuyễn\"," +
                "\"Pha sốt: 2 muỗng dấm + 3 muỗng đường + 2 muỗng nước mắm + chút nước\"," +
                "\"Cho sốt vào chảo, nấu sôi rồi cho sườn vào đảo đều\"," +
                "\"Thêm hành tây, ớt chuông xào nhanh, nêm nếm vừa ăn\"]",
                131000);

        createSystemRecipe("Canh Chua Cá Lóc", "Canh chua cá lóc nấu me, thơm, giá đỗ - đặc trưng miền Nam", "/uploads/seed/canh_chua.jpg",
                "[{\"name\":\"Cá lóc\",\"quantity\":\"1 con (~500g)\",\"estimated_price\":65000}," +
                "{\"name\":\"Thơm (dứa)\",\"quantity\":\"1/4 trái\",\"estimated_price\":8000}," +
                "{\"name\":\"Cà chua\",\"quantity\":\"2 trái\",\"estimated_price\":10000}," +
                "{\"name\":\"Giá đỗ\",\"quantity\":\"200g\",\"estimated_price\":8000}," +
                "{\"name\":\"Đậu bắp\",\"quantity\":\"100g\",\"estimated_price\":8000}," +
                "{\"name\":\"Rau ngổ\",\"quantity\":\"1 bó\",\"estimated_price\":5000}," +
                "{\"name\":\"Me chua\",\"quantity\":\"50g\",\"estimated_price\":10000}," +
                "{\"name\":\"Gia vị\",\"quantity\":\"nước mắm, đường, muối\",\"estimated_price\":10000}]",
                "[\"Cá lóc làm sạch, cắt khúc vừa ăn, ướp muối + nghệ\"," +
                "\"Me ngâm nước ấm, vắt lấy nước cốt me\"," +
                "\"Đun nước sôi, cho nước me + cà chua + thơm vào nấu\"," +
                "\"Khi nước sôi trở lại, cho cá vào, nấu 5-7 phút\"," +
                "\"Thêm đậu bắp, giá đỗ, nêm nước mắm + đường vừa ăn\"," +
                "\"Tắt bếp, rắc rau ngổ lên trên, ăn nóng với cơm\"]",
                124000);

        createSystemRecipe("Thịt Kho Trứng", "Thịt heo kho trứng cút nước dừa - món Tết cổ truyền miền Nam", "/uploads/seed/thit_kho_trung.jpg",
                "[{\"name\":\"Thịt ba chỉ\",\"quantity\":\"500g\",\"estimated_price\":75000}," +
                "{\"name\":\"Trứng cút\",\"quantity\":\"20 trứng\",\"estimated_price\":25000}," +
                "{\"name\":\"Nước dừa tươi\",\"quantity\":\"500ml\",\"estimated_price\":15000}," +
                "{\"name\":\"Nước mắm\",\"quantity\":\"3 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Đường thắng\",\"quantity\":\"2 muỗng\",\"estimated_price\":3000}," +
                "{\"name\":\"Tỏi, hành tím\",\"quantity\":\"3 tép + 2 củ\",\"estimated_price\":5000}]",
                "[\"Thịt ba chỉ cắt khúc vuông, luộc sơ rửa sạch\"," +
                "\"Trứng cút luộc chín, bóc vỏ\"," +
                "\"Thắng đường vàng cánh gián, cho thịt vào đảo đều\"," +
                "\"Phi hành tỏi thơm, cho nước dừa + nước mắm vào\"," +
                "\"Đun sôi rồi hạ lửa nhỏ, kho 45-60 phút cho thịt mềm\"," +
                "\"Cho trứng cút vào, kho thêm 15 phút, nêm nếm vừa ăn\"]",
                128000);

        createSystemRecipe("Gà Xào Sả Ớt", "Gà xào sả ớt cay nồng, thơm lừng - nhậu hay cơm đều hợp", "/uploads/seed/ga_xao_sa_ot.jpg",
                "[{\"name\":\"Đùi gà\",\"quantity\":\"500g\",\"estimated_price\":55000}," +
                "{\"name\":\"Sả\",\"quantity\":\"5 cây\",\"estimated_price\":5000}," +
                "{\"name\":\"Ớt hiểm\",\"quantity\":\"5 trái\",\"estimated_price\":5000}," +
                "{\"name\":\"Tỏi\",\"quantity\":\"5 tép\",\"estimated_price\":3000}," +
                "{\"name\":\"Nước mắm\",\"quantity\":\"2 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Đường\",\"quantity\":\"1 muỗng\",\"estimated_price\":2000}]",
                "[\"Gà chặt miếng vừa ăn, ướp nước mắm + tiêu + tỏi 20 phút\"," +
                "\"Sả băm nhuyễn, ớt cắt lát\"," +
                "\"Phi tỏi + sả thơm vàng trên lửa lớn\"," +
                "\"Cho gà vào xào mạnh tay, đảo đều đến khi gà chín vàng\"," +
                "\"Thêm ớt, nêm nước mắm + đường, đảo thêm 2 phút\"," +
                "\"Tắt bếp, rắc tiêu + hành lá, dọn ra đĩa\"]",
                75000);

        createSystemRecipe("Bún Bò Huế", "Bún bò Huế chuẩn vị xứ Huế, nước lèo đậm đà sả ruốc", "/uploads/seed/bun_bo_hue.jpg",
                "[{\"name\":\"Bắp bò\",\"quantity\":\"500g\",\"estimated_price\":120000}," +
                "{\"name\":\"Giò heo\",\"quantity\":\"1 cái\",\"estimated_price\":45000}," +
                "{\"name\":\"Bún tươi\",\"quantity\":\"1kg\",\"estimated_price\":20000}," +
                "{\"name\":\"Sả\",\"quantity\":\"10 cây\",\"estimated_price\":10000}," +
                "{\"name\":\"Mắm ruốc\",\"quantity\":\"2 muỗng\",\"estimated_price\":10000}," +
                "{\"name\":\"Ớt bột\",\"quantity\":\"2 muỗng\",\"estimated_price\":8000}," +
                "{\"name\":\"Rau sống\",\"quantity\":\"rau muống bào, giá, bắp chuối\",\"estimated_price\":15000}]",
                "[\"Bắp bò, giò heo luộc sơ, rửa sạch\"," +
                "\"Nấu nước dùng: hầm xương bò 2-3 tiếng với sả đập dập\"," +
                "\"Pha mắm ruốc với ít nước dùng, lọc bỏ cặn, cho vào nồi\"," +
                "\"Phi dầu sả + ớt bột tạo màu, cho vào nồi nước dùng\"," +
                "\"Thái bò + giò heo, trụng bún nóng\"," +
                "\"Xếp bún vào tô, đặt thịt lên, chan nước dùng nóng, ăn kèm rau sống\"]",
                228000);

        createSystemRecipe("Phở Bò Hà Nội", "Phở bò truyền thống Hà Nội, nước dùng trong veo thơm nức", "/uploads/seed/pho_bo.jpg",
                "[{\"name\":\"Xương ống bò\",\"quantity\":\"1kg\",\"estimated_price\":80000}," +
                "{\"name\":\"Thịt bò tái/chín\",\"quantity\":\"300g\",\"estimated_price\":120000}," +
                "{\"name\":\"Bánh phở tươi\",\"quantity\":\"500g\",\"estimated_price\":15000}," +
                "{\"name\":\"Hành tây\",\"quantity\":\"1 củ\",\"estimated_price\":8000}," +
                "{\"name\":\"Gừng\",\"quantity\":\"1 củ\",\"estimated_price\":5000}," +
                "{\"name\":\"Quế, hồi, thảo quả\",\"quantity\":\"1 bộ\",\"estimated_price\":15000}," +
                "{\"name\":\"Hành lá, ngò gai\",\"quantity\":\"1 bó\",\"estimated_price\":5000}]",
                "[\"Xương bò rửa sạch, trần qua nước sôi, rửa lại\"," +
                "\"Nướng hành tây + gừng trên bếp cho thơm\"," +
                "\"Hầm xương 4-5 tiếng với quế, hồi, thảo quả trên lửa nhỏ\"," +
                "\"Lọc nước dùng trong, nêm nước mắm + muối + đường phèn\"," +
                "\"Trụng bánh phở qua nước sôi, xếp vào tô\"," +
                "\"Xếp thịt bò lên, chan nước dùng nóng, rắc hành lá + ngò gai\"]",
                248000);

        createSystemRecipe("Cơm Tấm Sườn Bì Chả", "Cơm tấm Sài Gòn đầy đủ sườn nướng, bì, chả trứng", "/uploads/seed/com_tam.jpg",
                "[{\"name\":\"Sườn heo\",\"quantity\":\"4 miếng\",\"estimated_price\":80000}," +
                "{\"name\":\"Gạo tấm\",\"quantity\":\"500g\",\"estimated_price\":15000}," +
                "{\"name\":\"Bì heo\",\"quantity\":\"200g\",\"estimated_price\":25000}," +
                "{\"name\":\"Trứng + thịt xay\",\"quantity\":\"3 trứng + 200g\",\"estimated_price\":35000}," +
                "{\"name\":\"Nước mắm, đường, tỏi\",\"quantity\":\"pha chế\",\"estimated_price\":10000}," +
                "{\"name\":\"Đồ chua, dưa leo\",\"quantity\":\"1 phần\",\"estimated_price\":10000}]",
                "[\"Sườn ướp sả + tỏi + nước mắm + mật ong 2 tiếng\"," +
                "\"Nướng sườn trên than hoa hoặc lò nướng đến vàng đều\"," +
                "\"Bì heo luộc chín, thái sợi, trộn thính gạo\"," +
                "\"Chả trứng: trộn thịt xay + trứng + mộc nhĩ, hấp 30 phút\"," +
                "\"Nấu cơm tấm bằng gạo tấm, hơi khô\"," +
                "\"Xếp đĩa: cơm + sườn + bì + chả, kèm nước mắm pha\"]",
                175000);

        createSystemRecipe("Bánh Mì Thịt Nướng", "Bánh mì Việt Nam nhân thịt nướng, đồ chua, rau thơm", "/uploads/seed/banh_mi.jpg",
                "[{\"name\":\"Bánh mì\",\"quantity\":\"4 ổ\",\"estimated_price\":16000}," +
                "{\"name\":\"Thịt heo nướng\",\"quantity\":\"400g\",\"estimated_price\":60000}," +
                "{\"name\":\"Pate gan\",\"quantity\":\"100g\",\"estimated_price\":15000}," +
                "{\"name\":\"Đồ chua (cà rốt, củ cải)\",\"quantity\":\"200g\",\"estimated_price\":10000}," +
                "{\"name\":\"Dưa leo, ngò, ớt\",\"quantity\":\"1 phần\",\"estimated_price\":8000}," +
                "{\"name\":\"Nước tương, maggi\",\"quantity\":\"pha chế\",\"estimated_price\":5000}]",
                "[\"Thịt heo thái lát mỏng, ướp sả + tỏi + nước mắm + mật ong\"," +
                "\"Nướng thịt trên than hoặc chảo đến vàng thơm\"," +
                "\"Làm đồ chua: cà rốt + củ cải ngâm giấm đường\"," +
                "\"Nướng bánh mì giòn, xẻ đôi\"," +
                "\"Phết pate, xếp thịt nướng, đồ chua, dưa leo, ngò\"," +
                "\"Rưới nước tương, thêm ớt tùy thích\"]",
                114000);

        createSystemRecipe("Bò Lúc Lắc", "Bò lúc lắc sốt tiêu đen, mềm ngọt thịt bò - món nhà hàng tại gia", "/uploads/seed/bo_luc_lac.jpg",
                "[{\"name\":\"Thịt bò thăn\",\"quantity\":\"400g\",\"estimated_price\":160000}," +
                "{\"name\":\"Ớt chuông\",\"quantity\":\"2 trái\",\"estimated_price\":20000}," +
                "{\"name\":\"Hành tây\",\"quantity\":\"1 củ\",\"estimated_price\":8000}," +
                "{\"name\":\"Tỏi\",\"quantity\":\"5 tép\",\"estimated_price\":3000}," +
                "{\"name\":\"Dầu hào + nước tương\",\"quantity\":\"2 muỗng mỗi loại\",\"estimated_price\":10000}," +
                "{\"name\":\"Bơ\",\"quantity\":\"20g\",\"estimated_price\":8000}]",
                "[\"Bò cắt hạt lựu vuông 2cm, ướp tỏi + dầu hào + tiêu 30 phút\"," +
                "\"Đun chảo thật nóng với dầu ăn, cho bò vào xào lửa lớn\"," +
                "\"Đảo nhanh tay 1-2 phút để bò chín tái, vớt ra\"," +
                "\"Xào hành tây + ớt chuông nhanh 1 phút\"," +
                "\"Cho bò lại vào, thêm bơ + nước tương, đảo đều\"," +
                "\"Dọn ra đĩa kèm cơm trắng hoặc xà lách\"]",
                209000);

        createSystemRecipe("Cá Kho Tộ", "Cá lóc kho tộ đất nước dừa - đậm đà hương vị miền Tây", "/uploads/seed/ca_kho_to.jpg",
                "[{\"name\":\"Cá lóc\",\"quantity\":\"1 con (~600g)\",\"estimated_price\":70000}," +
                "{\"name\":\"Nước dừa tươi\",\"quantity\":\"300ml\",\"estimated_price\":10000}," +
                "{\"name\":\"Nước màu (thắng đường)\",\"quantity\":\"2 muỗng\",\"estimated_price\":3000}," +
                "{\"name\":\"Hành tím, tỏi\",\"quantity\":\"3 củ + 3 tép\",\"estimated_price\":5000}," +
                "{\"name\":\"Nước mắm\",\"quantity\":\"3 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Tiêu, ớt\",\"quantity\":\"1 muỗng + 2 trái\",\"estimated_price\":5000}]",
                "[\"Cá lóc làm sạch, cắt khúc dày 3cm\"," +
                "\"Ướp cá với nước mắm + tiêu + hành tím băm 20 phút\"," +
                "\"Thắng đường trong tộ đất đến vàng cánh gián\"," +
                "\"Xếp cá vào tộ, đổ nước dừa ngập mặt cá\"," +
                "\"Kho lửa nhỏ 30-40 phút cho cá thấm đều, nước kho sánh\"," +
                "\"Rắc tiêu + hành lá, ăn nóng với cơm trắng\"]",
                98000);

        createSystemRecipe("Gỏi Cuốn Tôm Thịt", "Gỏi cuốn tôm thịt tươi mát, chấm mắm nêm hoặc tương đậu", "/uploads/seed/goi_cuon.jpg",
                "[{\"name\":\"Tôm sú\",\"quantity\":\"200g\",\"estimated_price\":60000}," +
                "{\"name\":\"Thịt ba chỉ\",\"quantity\":\"200g\",\"estimated_price\":35000}," +
                "{\"name\":\"Bánh tráng\",\"quantity\":\"20 cái\",\"estimated_price\":15000}," +
                "{\"name\":\"Bún tươi\",\"quantity\":\"200g\",\"estimated_price\":8000}," +
                "{\"name\":\"Rau sống các loại\",\"quantity\":\"1 mớ\",\"estimated_price\":10000}," +
                "{\"name\":\"Tương đậu phộng\",\"quantity\":\"100ml\",\"estimated_price\":12000}]",
                "[\"Tôm luộc chín, bóc vỏ, xẻ đôi\"," +
                "\"Thịt ba chỉ luộc chín, thái lát mỏng\"," +
                "\"Nhúng bánh tráng qua nước, trải ra\"," +
                "\"Xếp rau sống, bún, thịt, tôm lên bánh tráng\"," +
                "\"Cuốn chặt tay, gấp mép hai bên\"," +
                "\"Pha nước chấm tương đậu phộng: tương + đậu phộng rang giã\"]",
                140000);

        createSystemRecipe("Chả Giò (Nem Rán)", "Nem rán giòn rụm nhân thịt, miến, nấm - món khai vị kinh điển", "/uploads/seed/cha_gio.jpg",
                "[{\"name\":\"Thịt heo xay\",\"quantity\":\"300g\",\"estimated_price\":45000}," +
                "{\"name\":\"Miến dong\",\"quantity\":\"50g\",\"estimated_price\":10000}," +
                "{\"name\":\"Nấm mèo\",\"quantity\":\"30g\",\"estimated_price\":10000}," +
                "{\"name\":\"Cà rốt\",\"quantity\":\"1 củ\",\"estimated_price\":5000}," +
                "{\"name\":\"Bánh tráng nem\",\"quantity\":\"20 cái\",\"estimated_price\":15000}," +
                "{\"name\":\"Trứng\",\"quantity\":\"1 quả\",\"estimated_price\":4000}]",
                "[\"Miến ngâm mềm cắt ngắn, nấm mèo ngâm thái nhỏ\"," +
                "\"Trộn thịt xay + miến + nấm + cà rốt bào + trứng + gia vị\"," +
                "\"Cuốn nhân vào bánh tráng nem, cuộn chặt\"," +
                "\"Chiên ngập dầu lửa vừa đến vàng giòn đều\"," +
                "\"Vớt ra giấy thấm dầu\"," +
                "\"Ăn kèm bún, rau sống, nước mắm chua ngọt\"]",
                89000);

        createSystemRecipe("Bánh Xèo Miền Tây", "Bánh xèo giòn rụm nhân tôm thịt, giá đỗ - đặc sản miền Tây", "/uploads/seed/banh_xeo.jpg",
                "[{\"name\":\"Bột gạo\",\"quantity\":\"300g\",\"estimated_price\":12000}," +
                "{\"name\":\"Bột nghệ\",\"quantity\":\"1 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Nước cốt dừa\",\"quantity\":\"200ml\",\"estimated_price\":15000}," +
                "{\"name\":\"Tôm sú\",\"quantity\":\"200g\",\"estimated_price\":60000}," +
                "{\"name\":\"Thịt ba chỉ\",\"quantity\":\"200g\",\"estimated_price\":35000}," +
                "{\"name\":\"Giá đỗ\",\"quantity\":\"200g\",\"estimated_price\":8000}," +
                "{\"name\":\"Rau sống cuốn\",\"quantity\":\"1 mớ\",\"estimated_price\":10000}]",
                "[\"Pha bột: bột gạo + nghệ + nước cốt dừa + nước, khuấy mịn\"," +
                "\"Tôm bóc vỏ, thịt thái lát mỏng\"," +
                "\"Chảo nóng dầu, cho tôm + thịt vào xào sơ\"," +
                "\"Đổ bột vào chảo, xoay tròn đều, đậy nắp 2 phút\"," +
                "\"Cho giá đỗ vào, gập đôi bánh, chiên thêm 1 phút cho giòn\"," +
                "\"Ăn kèm rau sống cuốn, chấm nước mắm chua ngọt\"]",
                145000);

        createSystemRecipe("Lẩu Thái Hải Sản", "Lẩu Thái chua cay với tôm, mực, cá - nóng hổi cho cả nhà", "/uploads/seed/lau_thai.jpg",
                "[{\"name\":\"Tôm sú\",\"quantity\":\"300g\",\"estimated_price\":90000}," +
                "{\"name\":\"Mực ống\",\"quantity\":\"200g\",\"estimated_price\":60000}," +
                "{\"name\":\"Cá basa\",\"quantity\":\"300g\",\"estimated_price\":40000}," +
                "{\"name\":\"Nấm kim châm, nấm bào ngư\",\"quantity\":\"200g\",\"estimated_price\":25000}," +
                "{\"name\":\"Gia vị lẩu Thái\",\"quantity\":\"1 gói\",\"estimated_price\":20000}," +
                "{\"name\":\"Rau ăn lẩu\",\"quantity\":\"rau muống, bắp cải, cải thảo\",\"estimated_price\":20000}," +
                "{\"name\":\"Bún tươi\",\"quantity\":\"500g\",\"estimated_price\":12000}]",
                "[\"Hải sản rửa sạch, mực khía vân, cá cắt khúc\"," +
                "\"Nấu nước lẩu: gia vị lẩu Thái + nước dùng + sả + lá chanh\"," +
                "\"Khi nước sôi, cho nấm vào trước\"," +
                "\"Nhúng hải sản vào lẩu, nấu vừa chín tới\"," +
                "\"Nhúng rau, bún vào ăn kèm\"," +
                "\"Nêm thêm chanh + ớt tùy khẩu vị\"]",
                267000);

        createSystemRecipe("Cơm Chiên Dương Châu", "Cơm chiên đầy đủ tôm, chả lụa, trứng, rau củ - nhanh gọn ngon", "/uploads/seed/com_chien.jpg",
                "[{\"name\":\"Cơm nguội\",\"quantity\":\"4 chén\",\"estimated_price\":10000}," +
                "{\"name\":\"Tôm\",\"quantity\":\"150g\",\"estimated_price\":40000}," +
                "{\"name\":\"Chả lụa\",\"quantity\":\"100g\",\"estimated_price\":20000}," +
                "{\"name\":\"Trứng\",\"quantity\":\"3 quả\",\"estimated_price\":12000}," +
                "{\"name\":\"Cà rốt, đậu Hà Lan\",\"quantity\":\"100g\",\"estimated_price\":10000}," +
                "{\"name\":\"Hành lá\",\"quantity\":\"1 bó\",\"estimated_price\":3000}]",
                "[\"Tôm bóc vỏ, chả lụa cắt hạt lựu nhỏ\"," +
                "\"Đánh trứng, chiên thành miếng mỏng, cắt sợi\"," +
                "\"Chảo nóng dầu, xào tôm + chả + cà rốt + đậu\"," +
                "\"Cho cơm nguội vào, đảo lửa lớn liên tục\"," +
                "\"Nêm nước mắm + tiêu + hạt nêm, đảo đều\"," +
                "\"Cho trứng + hành lá vào, đảo nhanh, tắt bếp\"]",
                95000);

        createSystemRecipe("Bò Kho", "Bò kho sốt cà, thơm lừng quế hồi - ăn với bánh mì hoặc bún", "/uploads/seed/bo_kho.jpg",
                "[{\"name\":\"Thịt bò bắp\",\"quantity\":\"500g\",\"estimated_price\":150000}," +
                "{\"name\":\"Cà rốt\",\"quantity\":\"2 củ\",\"estimated_price\":10000}," +
                "{\"name\":\"Khoai tây\",\"quantity\":\"2 củ\",\"estimated_price\":10000}," +
                "{\"name\":\"Cà chua\",\"quantity\":\"3 trái\",\"estimated_price\":15000}," +
                "{\"name\":\"Sả, gừng\",\"quantity\":\"3 cây + 1 củ\",\"estimated_price\":8000}," +
                "{\"name\":\"Quế, hồi, bột cà ri\",\"quantity\":\"1 bộ\",\"estimated_price\":15000}]",
                "[\"Bò cắt khối vuông 3cm, ướp sả + bột cà ri + nước mắm 1 tiếng\"," +
                "\"Phi hành tỏi thơm, cho bò vào xào săn mặt ngoài\"," +
                "\"Thêm cà chua xào nhuyễn, cho nước sôi ngập bò\"," +
                "\"Cho quế, hồi, gừng nướng vào, hầm lửa nhỏ 1.5-2 tiếng\"," +
                "\"Cho cà rốt + khoai tây vào, hầm thêm 20 phút\"," +
                "\"Nêm nếm vừa ăn, ăn kèm bánh mì hoặc bún\"]",
                208000);

        createSystemRecipe("Bánh Cuốn Thanh Trì", "Bánh cuốn mỏng mịn nhân thịt nấm, chấm nước mắm chua ngọt", "/uploads/seed/banh_cuon.jpg",
                "[{\"name\":\"Bột gạo\",\"quantity\":\"300g\",\"estimated_price\":12000}," +
                "{\"name\":\"Bột năng\",\"quantity\":\"50g\",\"estimated_price\":5000}," +
                "{\"name\":\"Thịt heo xay\",\"quantity\":\"200g\",\"estimated_price\":35000}," +
                "{\"name\":\"Nấm mèo\",\"quantity\":\"30g\",\"estimated_price\":10000}," +
                "{\"name\":\"Hành khô\",\"quantity\":\"3 củ\",\"estimated_price\":5000}," +
                "{\"name\":\"Chả lụa, giò\",\"quantity\":\"100g\",\"estimated_price\":20000}]",
                "[\"Pha bột: bột gạo + bột năng + nước, khuấy loãng mịn\"," +
                "\"Xào nhân: thịt xay + nấm mèo + hành khô phi\"," +
                "\"Tráng bột mỏng trên nồi hấp (hoặc chảo chống dính)\"," +
                "\"Khi bột chín trong, gỡ ra, cho nhân vào cuộn\"," +
                "\"Xếp bánh cuốn ra đĩa, rắc hành phi\"," +
                "\"Ăn kèm chả lụa, rau thơm, nước mắm chua ngọt\"]",
                87000);

        createSystemRecipe("Mì Quảng", "Mì Quảng nước lèo tôm thịt, đậm đà phong vị miền Trung", "/uploads/seed/mi_quang.jpg",
                "[{\"name\":\"Mì Quảng tươi\",\"quantity\":\"500g\",\"estimated_price\":15000}," +
                "{\"name\":\"Tôm sú\",\"quantity\":\"200g\",\"estimated_price\":60000}," +
                "{\"name\":\"Thịt heo\",\"quantity\":\"200g\",\"estimated_price\":35000}," +
                "{\"name\":\"Đậu phộng rang\",\"quantity\":\"100g\",\"estimated_price\":15000}," +
                "{\"name\":\"Nghệ tươi\",\"quantity\":\"1 củ\",\"estimated_price\":5000}," +
                "{\"name\":\"Rau sống, bánh tráng\",\"quantity\":\"1 mớ\",\"estimated_price\":15000}]",
                "[\"Tôm lột vỏ, thịt thái lát, ướp nghệ + nước mắm\"," +
                "\"Xào tôm + thịt với nghệ cho thơm, vớt ra\"," +
                "\"Nấu nước lèo từ đầu tôm + xương, nêm vừa ăn\"," +
                "\"Trụng mì qua nước sôi, xếp vào tô\"," +
                "\"Xếp tôm, thịt lên, chan nước lèo vừa xăm xắp\"," +
                "\"Rắc đậu phộng rang, ăn kèm bánh tráng + rau sống\"]",
                145000);

        createSystemRecipe("Gà Nướng Muối Ớt", "Gà nướng muối ớt vàng ươm, giòn da mềm thịt", "/uploads/seed/ga_nuong.jpg",
                "[{\"name\":\"Gà ta nguyên con\",\"quantity\":\"1 con (~1.2kg)\",\"estimated_price\":150000}," +
                "{\"name\":\"Muối hạt\",\"quantity\":\"3 muỗng\",\"estimated_price\":3000}," +
                "{\"name\":\"Ớt bột\",\"quantity\":\"2 muỗng\",\"estimated_price\":8000}," +
                "{\"name\":\"Tỏi\",\"quantity\":\"1 củ\",\"estimated_price\":5000}," +
                "{\"name\":\"Mật ong\",\"quantity\":\"2 muỗng\",\"estimated_price\":15000}," +
                "{\"name\":\"Chanh, lá chanh\",\"quantity\":\"2 trái + 5 lá\",\"estimated_price\":5000}]",
                "[\"Gà rửa sạch, để ráo, rạch vài đường trên da\"," +
                "\"Pha muối ớt: muối + ớt bột + tỏi băm + mật ong + nước chanh\"," +
                "\"Xoa đều hỗn hợp lên gà, ướp 2-3 tiếng (hoặc qua đêm)\"," +
                "\"Nhồi lá chanh vào bụng gà\"," +
                "\"Nướng 180°C trong 50-60 phút, trở mặt giữa chừng\"," +
                "\"Phết thêm mật ong lên da gà 10 phút cuối cho vàng giòn\"]",
                186000);

        createSystemRecipe("Bún Chả Hà Nội", "Bún chả thơm lừng than hoa, nước mắm chua ngọt chuẩn Hà Nội", "/uploads/seed/bun_cha.jpg",
                "[{\"name\":\"Thịt ba chỉ\",\"quantity\":\"300g\",\"estimated_price\":50000}," +
                "{\"name\":\"Thịt nạc vai xay\",\"quantity\":\"200g\",\"estimated_price\":40000}," +
                "{\"name\":\"Bún tươi\",\"quantity\":\"500g\",\"estimated_price\":12000}," +
                "{\"name\":\"Nước mắm\",\"quantity\":\"3 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Đường, giấm, tỏi, ớt\",\"quantity\":\"pha chế\",\"estimated_price\":10000}," +
                "{\"name\":\"Rau sống, đu đủ xanh\",\"quantity\":\"1 mớ\",\"estimated_price\":10000}]",
                "[\"Thịt ba chỉ thái lát mỏng, ướp nước mắm + đường + tiêu\"," +
                "\"Thịt xay viên tròn nhỏ, ướp tương tự\"," +
                "\"Nướng thịt trên than hoa đến vàng thơm\"," +
                "\"Pha nước chấm: nước mắm + đường + giấm + tỏi + ớt + nước ấm\"," +
                "\"Cho đu đủ xanh bào sợi vào nước chấm\"," +
                "\"Bày bún ra đĩa, thịt nướng vào bát nước chấm, ăn kèm rau sống\"]",
                127000);

        // ═══════════════════════════════════════════════
        // 10 MÓN CỘNG ĐỒNG (Community Recipes)
        // ═══════════════════════════════════════════════

        createCommunityRecipe(userId, "Bún Riêu Cua Đồng", "Bún riêu cua đồng chua thanh, đậu phụ rán vàng - ngon nhớ mãi", "/uploads/seed/bun_rieu.jpg",
                "[{\"name\":\"Cua đồng xay\",\"quantity\":\"500g\",\"estimated_price\":40000}," +
                "{\"name\":\"Cà chua\",\"quantity\":\"4 trái\",\"estimated_price\":15000}," +
                "{\"name\":\"Đậu phụ\",\"quantity\":\"4 bìa\",\"estimated_price\":12000}," +
                "{\"name\":\"Bún tươi\",\"quantity\":\"500g\",\"estimated_price\":12000}," +
                "{\"name\":\"Mắm tôm\",\"quantity\":\"1 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Rau sống, giá đỗ\",\"quantity\":\"1 mớ\",\"estimated_price\":10000}]",
                "[\"Cua xay lọc lấy nước cốt, gạch cua để riêng\"," +
                "\"Đun nước cua lửa nhỏ, gạch cua nổi lên vớt ra\"," +
                "\"Phi hành + cà chua xào nhuyễn, cho vào nồi nước cua\"," +
                "\"Đậu phụ rán vàng, cắt tam giác\"," +
                "\"Nêm mắm tôm + muối + đường vừa ăn\"," +
                "\"Trụng bún, xếp tô, chan nước riêu, thêm đậu phụ + rau\"]",
                94000);

        createCommunityRecipe(userId, "Bánh Khọt Vũng Tàu", "Bánh khọt giòn rụm nhân tôm, chấm mắm nêm - đặc sản miền Nam", "/uploads/seed/banh_khot.jpg",
                "[{\"name\":\"Bột gạo\",\"quantity\":\"200g\",\"estimated_price\":8000}," +
                "{\"name\":\"Nước cốt dừa\",\"quantity\":\"200ml\",\"estimated_price\":15000}," +
                "{\"name\":\"Tôm tươi\",\"quantity\":\"200g\",\"estimated_price\":50000}," +
                "{\"name\":\"Hành lá\",\"quantity\":\"1 bó\",\"estimated_price\":3000}," +
                "{\"name\":\"Nghệ bột\",\"quantity\":\"1 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Rau sống cuốn\",\"quantity\":\"1 mớ\",\"estimated_price\":10000}]",
                "[\"Pha bột: bột gạo + nước cốt dừa + nghệ + nước, khuấy mịn\"," +
                "\"Tôm bóc vỏ, để nguyên con\"," +
                "\"Khuôn bánh khọt nóng, thoa dầu, đổ bột vào\"," +
                "\"Đặt 1 con tôm lên mỗi bánh, đậy nắp 3-4 phút\"," +
                "\"Khi bánh giòn cạnh, vàng đều thì gỡ ra\"," +
                "\"Cuốn bánh với rau sống, chấm mắm nêm pha\"]",
                91000);

        createCommunityRecipe(userId, "Cơm Gà Hội An", "Cơm gà Hội An xé phay, cơm nấu nước luộc gà thơm béo", "/uploads/seed/com_ga.jpg",
                "[{\"name\":\"Gà ta\",\"quantity\":\"1/2 con (~600g)\",\"estimated_price\":90000}," +
                "{\"name\":\"Gạo dẻo\",\"quantity\":\"400g\",\"estimated_price\":12000}," +
                "{\"name\":\"Nghệ tươi\",\"quantity\":\"1 củ\",\"estimated_price\":5000}," +
                "{\"name\":\"Hành tây, hành lá\",\"quantity\":\"1 củ + 1 bó\",\"estimated_price\":8000}," +
                "{\"name\":\"Rau răm, húng quế\",\"quantity\":\"1 mớ\",\"estimated_price\":5000}," +
                "{\"name\":\"Ớt, tỏi, gừng\",\"quantity\":\"pha chế\",\"estimated_price\":8000}]",
                "[\"Gà luộc chín, để nguội, xé phay sợi nhỏ\"," +
                "\"Nước luộc gà lọc trong, dùng nấu cơm thay nước\"," +
                "\"Vo gạo, nấu với nước luộc gà + nghệ cho cơm vàng ươm\"," +
                "\"Trộn gà xé với hành tây thái mỏng + rau răm\"," +
                "\"Pha nước mắm gừng: mắm + đường + gừng + ớt + tỏi\"," +
                "\"Xúc cơm ra đĩa, xếp gà lên, rưới nước mắm gừng\"]",
                128000);

        createCommunityRecipe(userId, "Chè Ba Màu", "Chè ba màu mát lạnh - đậu đỏ, đậu xanh, thạch lá dứa", "/uploads/seed/che_ba_mau.jpg",
                "[{\"name\":\"Đậu đỏ\",\"quantity\":\"200g\",\"estimated_price\":20000}," +
                "{\"name\":\"Đậu xanh cà\",\"quantity\":\"200g\",\"estimated_price\":18000}," +
                "{\"name\":\"Bột rau câu\",\"quantity\":\"1 gói\",\"estimated_price\":8000}," +
                "{\"name\":\"Lá dứa\",\"quantity\":\"5 lá\",\"estimated_price\":3000}," +
                "{\"name\":\"Nước cốt dừa\",\"quantity\":\"200ml\",\"estimated_price\":15000}," +
                "{\"name\":\"Đường\",\"quantity\":\"200g\",\"estimated_price\":8000}]",
                "[\"Đậu đỏ ngâm 4 tiếng, nấu mềm với đường\"," +
                "\"Đậu xanh hấp chín, tán nhuyễn, trộn đường\"," +
                "\"Nấu thạch lá dứa: nước lá dứa + bột rau câu + đường\"," +
                "\"Đổ thạch ra khuôn, để nguội, cắt hạt lựu\"," +
                "\"Xếp lớp vào ly: đậu đỏ → đậu xanh → thạch\"," +
                "\"Rưới nước cốt dừa + đá bào lên trên\"]",
                72000);

        createCommunityRecipe(userId, "Xôi Xéo Hà Nội", "Xôi xéo đậu xanh hành phi thơm lừng - bữa sáng kinh điển Hà Nội", "/uploads/seed/xoi.jpg",
                "[{\"name\":\"Nếp cái hoa vàng\",\"quantity\":\"500g\",\"estimated_price\":25000}," +
                "{\"name\":\"Đậu xanh cà\",\"quantity\":\"200g\",\"estimated_price\":18000}," +
                "{\"name\":\"Hành tím\",\"quantity\":\"5 củ\",\"estimated_price\":8000}," +
                "{\"name\":\"Mỡ hành\",\"quantity\":\"50ml\",\"estimated_price\":5000}," +
                "{\"name\":\"Nghệ bột\",\"quantity\":\"1 muỗng\",\"estimated_price\":3000}," +
                "{\"name\":\"Muối, đường\",\"quantity\":\"vừa ăn\",\"estimated_price\":3000}]",
                "[\"Nếp ngâm 4 tiếng, trộn nghệ cho vàng, hấp chín\"," +
                "\"Đậu xanh hấp chín, tán nhuyễn, nắm viên\"," +
                "\"Phi hành tím giòn vàng, giữ mỡ hành\"," +
                "\"Xúc xôi ra, đặt đậu xanh lên trên\"," +
                "\"Rưới mỡ hành, rắc hành phi giòn\"," +
                "\"Ăn nóng, thêm chút muối vừng càng ngon\"]",
                62000);

        createCommunityRecipe(userId, "Cà Ri Gà Nước Cốt Dừa", "Cà ri gà kiểu Việt Nam, nước cốt dừa béo ngậy, ăn với bánh mì", "/uploads/seed/ca_ri_ga.jpg",
                "[{\"name\":\"Đùi gà\",\"quantity\":\"500g\",\"estimated_price\":55000}," +
                "{\"name\":\"Khoai tây\",\"quantity\":\"2 củ\",\"estimated_price\":10000}," +
                "{\"name\":\"Cà rốt\",\"quantity\":\"1 củ\",\"estimated_price\":5000}," +
                "{\"name\":\"Nước cốt dừa\",\"quantity\":\"400ml\",\"estimated_price\":20000}," +
                "{\"name\":\"Bột cà ri\",\"quantity\":\"3 muỗng\",\"estimated_price\":15000}," +
                "{\"name\":\"Sả, hành tây\",\"quantity\":\"3 cây + 1 củ\",\"estimated_price\":8000}]",
                "[\"Gà chặt miếng, ướp bột cà ri + nước mắm 30 phút\"," +
                "\"Phi sả + hành tây thơm, cho gà vào xào săn\"," +
                "\"Thêm khoai tây + cà rốt cắt khối, xào đều\"," +
                "\"Đổ nước cốt dừa vào, thêm nước vừa ngập\"," +
                "\"Hầm lửa nhỏ 30-40 phút cho gà mềm, khoai chín\"," +
                "\"Nêm nếm, ăn kèm bánh mì hoặc bún\"]",
                113000);

        createCommunityRecipe(userId, "Súp Cua Trứng Bắc Thảo", "Súp cua thơm ngọt, sánh mịn - món khai vị sang trọng", "/uploads/seed/sup_cua.jpg",
                "[{\"name\":\"Thịt cua\",\"quantity\":\"200g\",\"estimated_price\":80000}," +
                "{\"name\":\"Trứng bắc thảo\",\"quantity\":\"2 quả\",\"estimated_price\":20000}," +
                "{\"name\":\"Trứng gà\",\"quantity\":\"2 quả\",\"estimated_price\":8000}," +
                "{\"name\":\"Bột năng\",\"quantity\":\"3 muỗng\",\"estimated_price\":5000}," +
                "{\"name\":\"Nấm rơm\",\"quantity\":\"100g\",\"estimated_price\":15000}," +
                "{\"name\":\"Hành ngò\",\"quantity\":\"1 bó\",\"estimated_price\":3000}]",
                "[\"Thịt cua gỡ nhỏ, trứng bắc thảo cắt hạt lựu\"," +
                "\"Nấu nước dùng gà, cho nấm rơm vào\"," +
                "\"Cho thịt cua + trứng bắc thảo vào nồi\"," +
                "\"Pha bột năng với nước lạnh, đổ từ từ vào khuấy đều\"," +
                "\"Đánh trứng gà, rưới từ từ vào nồi tạo sợi\"," +
                "\"Nêm tiêu + hành ngò, múc ra bát ăn nóng\"]",
                131000);

        createCommunityRecipe(userId, "Hủ Tiếu Nam Vang", "Hủ tiếu nước lèo trong veo, thịt bằm tôm gan - phong cách Sài Gòn", "/uploads/seed/hu_tieu.jpg",
                "[{\"name\":\"Hủ tiếu khô\",\"quantity\":\"400g\",\"estimated_price\":20000}," +
                "{\"name\":\"Thịt heo bằm\",\"quantity\":\"200g\",\"estimated_price\":35000}," +
                "{\"name\":\"Tôm\",\"quantity\":\"150g\",\"estimated_price\":45000}," +
                "{\"name\":\"Gan heo\",\"quantity\":\"100g\",\"estimated_price\":15000}," +
                "{\"name\":\"Xương heo\",\"quantity\":\"500g\",\"estimated_price\":30000}," +
                "{\"name\":\"Giá đỗ, hẹ, hành phi\",\"quantity\":\"1 mớ\",\"estimated_price\":10000}]",
                "[\"Hầm xương heo 2-3 tiếng lấy nước dùng trong\"," +
                "\"Thịt bằm viên nhỏ, tôm bóc vỏ, gan thái lát\"," +
                "\"Trụng hủ tiếu qua nước sôi, xếp vào tô\"," +
                "\"Xếp thịt, tôm, gan lên hủ tiếu\"," +
                "\"Chan nước dùng nóng, thêm giá đỗ + hẹ\"," +
                "\"Rắc hành phi + tiêu, ăn kèm tương ớt\"]",
                155000);

        createCommunityRecipe(userId, "Bánh Tráng Trộn Sài Gòn", "Bánh tráng trộn đầy đủ topping - món ăn vặt đường phố số 1", "/uploads/seed/banh_trang_tron.jpg",
                "[{\"name\":\"Bánh tráng cắt sợi\",\"quantity\":\"200g\",\"estimated_price\":15000}," +
                "{\"name\":\"Khô bò\",\"quantity\":\"50g\",\"estimated_price\":25000}," +
                "{\"name\":\"Trứng cút luộc\",\"quantity\":\"5 quả\",\"estimated_price\":10000}," +
                "{\"name\":\"Xoài xanh bào\",\"quantity\":\"1/2 trái\",\"estimated_price\":8000}," +
                "{\"name\":\"Sa tế, mắm ruốc\",\"quantity\":\"2 muỗng mỗi loại\",\"estimated_price\":10000}," +
                "{\"name\":\"Đậu phộng rang\",\"quantity\":\"50g\",\"estimated_price\":8000}]",
                "[\"Bánh tráng cắt sợi cho vào tô lớn\"," +
                "\"Thêm khô bò xé sợi, trứng cút cắt đôi\"," +
                "\"Cho xoài xanh bào sợi, rau răm\"," +
                "\"Thêm sa tế + mắm ruốc + nước tương\"," +
                "\"Rắc đậu phộng rang giã dập\"," +
                "\"Trộn đều tất cả, ăn ngay khi còn giòn\"]",
                76000);

        createCommunityRecipe(userId, "Nem Chua Rán Hà Nội", "Nem chua rán giòn ngoài mềm trong - món nhậu bình dân tuyệt vời", "/uploads/seed/nem_chua.jpg",
                "[{\"name\":\"Nem chua Thanh Hóa\",\"quantity\":\"20 cái\",\"estimated_price\":40000}," +
                "{\"name\":\"Trứng gà\",\"quantity\":\"2 quả\",\"estimated_price\":8000}," +
                "{\"name\":\"Bột chiên giòn\",\"quantity\":\"100g\",\"estimated_price\":10000}," +
                "{\"name\":\"Tương ớt, tương cà\",\"quantity\":\"pha chế\",\"estimated_price\":8000}," +
                "{\"name\":\"Rau sống\",\"quantity\":\"1 mớ\",\"estimated_price\":5000}," +
                "{\"name\":\"Dầu ăn\",\"quantity\":\"500ml\",\"estimated_price\":15000}]",
                "[\"Nem chua bóc lá, để nguyên hoặc cắt đôi\"," +
                "\"Đánh trứng, pha bột chiên giòn với nước vừa sánh\"," +
                "\"Nhúng nem vào trứng, lăn qua bột chiên giòn\"," +
                "\"Chiên ngập dầu nóng 170°C đến vàng giòn\"," +
                "\"Vớt ra giấy thấm dầu\"," +
                "\"Ăn nóng kèm tương ớt, rau sống\"]",
                86000);
    }

    private void createSystemRecipe(String title, String desc, String imageUrl, String ingredients, String steps, double cost) {
        CookbookRecipe r = new CookbookRecipe();
        r.setTitle(title);
        r.setDescription(desc);
        r.setImageUrl(imageUrl);
        r.setIngredientsJson(ingredients);
        r.setStepsJson(steps);
        r.setTotalCost(cost);
        r.setIsSystemRecipe(true);
        recipeRepository.save(r);
    }

    private void createCommunityRecipe(Long userId, String title, String desc, String imageUrl, String ingredients, String steps, double cost) {
        CookbookRecipe r = new CookbookRecipe();
        r.setUserId(userId);
        r.setTitle(title);
        r.setDescription(desc);
        r.setImageUrl(imageUrl);
        r.setIngredientsJson(ingredients);
        r.setStepsJson(steps);
        r.setTotalCost(cost);
        r.setIsSystemRecipe(false);
        recipeRepository.save(r);
    }

    private void enrichRecipe(CookbookRecipe recipe, Long viewerUserId) {
        if (recipe.getUserId() != null) {
            userRepository.findById(recipe.getUserId()).ifPresent(user ->
                    recipe.setAuthorName(user.getFullName()));
        } else {
            recipe.setAuthorName("GoMarket");
        }
        recipe.setLikeCount(likeRepository.countByRecipeId(recipe.getId()));
        recipe.setCommentCount(commentRepository.countByRecipeId(recipe.getId()));
        if (viewerUserId != null) {
            recipe.setIsLikedByUser(likeRepository.existsByRecipeIdAndUserId(recipe.getId(), viewerUserId));
        }
    }

    private void enrichComment(CookbookComment comment) {
        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            comment.setAuthorName(user.getFullName());
            comment.setAuthorAvatar(user.getAvatarUrl());
        });
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
