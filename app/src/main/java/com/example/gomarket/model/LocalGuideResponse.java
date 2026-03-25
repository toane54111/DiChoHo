package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response từ AI Thổ Địa — gợi ý đặc sản theo mùa vụ, vị trí, khẩu vị
 */
public class LocalGuideResponse {

    @SerializedName("locationLabel")
    private String locationLabel;

    @SerializedName("seasonLabel")
    private String seasonLabel;

    @SerializedName("tasteProfile")
    private String tasteProfile;

    @SerializedName("suggestions")
    private List<SuggestionItem> suggestions;

    public static class SuggestionItem {
        @SerializedName("name")
        private String name;

        @SerializedName("reason")
        private String reason;

        @SerializedName("emoji")
        private String emoji;

        @SerializedName("matchedPosts")
        private List<CommunityPost> matchedPosts;

        @SerializedName("hasResults")
        private boolean hasResults;

        public String getName() { return name; }
        public String getReason() { return reason; }
        public String getEmoji() { return emoji; }
        public List<CommunityPost> getMatchedPosts() { return matchedPosts; }
        public boolean isHasResults() { return hasResults; }
    }

    public String getLocationLabel() { return locationLabel; }
    public String getSeasonLabel() { return seasonLabel; }
    public String getTasteProfile() { return tasteProfile; }
    public List<SuggestionItem> getSuggestions() { return suggestions; }
}
