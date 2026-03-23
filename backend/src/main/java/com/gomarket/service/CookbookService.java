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

    @Transactional
    public void seedSystemRecipes() {
        // Only seed if no system recipes exist
        long count = recipeRepository.findByIsSystemRecipeTrueOrderByCreatedAtDesc(PageRequest.of(0, 1))
                .getTotalElements();
        if (count > 0) return;

        createSystemRecipe("Sườn Xào Chua Ngọt", "Sườn heo non xào sốt chua ngọt đậm đà, thơm lừng",
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

        createSystemRecipe("Canh Chua Cá Lóc", "Canh chua cá lóc nấu me, thơm, giá đỗ - đặc trưng miền Nam",
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

        createSystemRecipe("Thịt Kho Trứng", "Thịt heo kho trứng cút nước dừa - món Tết cổ truyền miền Nam",
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

        createSystemRecipe("Gà Xào Sả Ớt", "Gà xào sả ớt cay nồng, thơm lừng - nhậu hay cơm đều hợp",
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

        createSystemRecipe("Bún Bò Huế", "Bún bò Huế chuẩn vị xứ Huế, nước lèo đậm đà sả ruốc",
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
    }

    private void createSystemRecipe(String title, String desc, String ingredients, String steps, double cost) {
        CookbookRecipe r = new CookbookRecipe();
        r.setTitle(title);
        r.setDescription(desc);
        r.setIngredientsJson(ingredients);
        r.setStepsJson(steps);
        r.setTotalCost(cost);
        r.setIsSystemRecipe(true);
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
