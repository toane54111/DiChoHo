package com.gomarket.dto;

import com.gomarket.model.Post;
import java.util.List;

/**
 * Response cho AI Thổ Địa — gợi ý đặc sản theo mùa, vị trí và khẩu vị
 */
public class LocalGuideResponse {

    private String locationLabel;      // "TP.HCM" / "Hà Nội"
    private String seasonLabel;        // "Mùa Sầu Riêng & Thốt Nốt"
    private String tasteProfile;       // "Hảo ngọt, Trái cây nhiệt đới"
    private List<SuggestionItem> suggestions;

    public static class SuggestionItem {
        private String name;           // "Sầu riêng Ri6"
        private String reason;         // "Đang vào mùa, phù hợp sở thích trái cây ngọt"
        private String emoji;          // "🍈"
        private List<Post> matchedPosts;  // Bài đăng match RAG (có thể rỗng)
        private boolean hasResults;    // true nếu có bài đăng, false → kích cầu

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getEmoji() { return emoji; }
        public void setEmoji(String emoji) { this.emoji = emoji; }
        public List<Post> getMatchedPosts() { return matchedPosts; }
        public void setMatchedPosts(List<Post> matchedPosts) { this.matchedPosts = matchedPosts; }
        public boolean isHasResults() { return hasResults; }
        public void setHasResults(boolean hasResults) { this.hasResults = hasResults; }
    }

    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }
    public String getSeasonLabel() { return seasonLabel; }
    public void setSeasonLabel(String seasonLabel) { this.seasonLabel = seasonLabel; }
    public String getTasteProfile() { return tasteProfile; }
    public void setTasteProfile(String tasteProfile) { this.tasteProfile = tasteProfile; }
    public List<SuggestionItem> getSuggestions() { return suggestions; }
    public void setSuggestions(List<SuggestionItem> suggestions) { this.suggestions = suggestions; }
}
