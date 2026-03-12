package com.gomarket.service;

import com.google.gson.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageSearchService {

    private static final Logger log = LoggerFactory.getLogger(ImageSearchService.class);

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * Search image for a Vietnamese dish name using Google Images.
     * Scrapes Google Images search results to extract image URLs.
     * Returns image URL or null if not found.
     */
    public String searchDishImage(String dishName) {
        try {
            String query = dishName + " món ăn";
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

            // Google Images search URL
            String searchUrl = "https://www.google.com/search?q=" + encodedQuery
                    + "&tbm=isch&hl=vi";

            Request request = new Request.Builder()
                    .url(searchUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Accept-Language", "vi-VN,vi;q=0.9,en;q=0.8")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String html = response.body().string();
                    String imageUrl = extractImageUrl(html);
                    if (imageUrl != null) {
                        log.info("Found Google image for '{}': {}", dishName, imageUrl);
                        return imageUrl;
                    }
                }
                log.debug("No image found for '{}'", dishName);
            }
        } catch (Exception e) {
            log.warn("Image search failed for '{}': {}", dishName, e.getMessage());
        }
        return null;
    }

    /**
     * Extract the first usable image URL from Google Images HTML response.
     * Google embeds image metadata as JSON arrays in the page source.
     */
    private String extractImageUrl(String html) {
        // Pattern 1: Google embeds image URLs in ["URL",width,height] format
        // Look for full-size image URLs (usually https://...jpg or png)
        Pattern pattern = Pattern.compile("\\[\"(https?://[^\"]+\\.(?:jpg|jpeg|png|webp)(?:\\?[^\"]*)?)\",[0-9]+,[0-9]+\\]");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String url = matcher.group(1);
            // Skip Google's own assets and tiny thumbnails
            if (!url.contains("gstatic.com") &&
                !url.contains("google.com") &&
                !url.contains("googleapis.com") &&
                !url.contains("logo") &&
                !url.contains("icon")) {
                return url;
            }
        }

        // Pattern 2: Look for image URLs in data-src or src attributes
        Pattern imgPattern = Pattern.compile("(?:data-src|src)=\"(https?://[^\"]+\\.(?:jpg|jpeg|png|webp)[^\"]*)\"");
        Matcher imgMatcher = imgPattern.matcher(html);

        while (imgMatcher.find()) {
            String url = imgMatcher.group(1);
            if (!url.contains("gstatic.com") &&
                !url.contains("google.com") &&
                !url.contains("googleapis.com") &&
                url.length() > 50) { // Skip short URLs (likely icons)
                return url;
            }
        }

        // Pattern 3: Look for URLs in the script data (Google often embeds in AF_initDataCallback)
        Pattern scriptPattern = Pattern.compile("\"(https?://[^\"]{50,}\\.(?:jpg|jpeg|png|webp)(?:\\?[^\"]*)?)\"");
        Matcher scriptMatcher = scriptPattern.matcher(html);

        while (scriptMatcher.find()) {
            String url = scriptMatcher.group(1);
            if (!url.contains("gstatic.com") &&
                !url.contains("google.com") &&
                !url.contains("googleapis.com") &&
                !url.contains("encrypted-tbn")) {
                // Unescape unicode and URL encoding
                url = url.replace("\\u003d", "=")
                         .replace("\\u0026", "&")
                         .replace("\\\\", "\\");
                return url;
            }
        }

        return null;
    }
}
