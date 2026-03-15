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
            .followRedirects(true)
            .build();

    /**
     * Search image for a Vietnamese dish name.
     * Strategy: Bing Images (primary) → Wikipedia (fallback)
     */
    public String searchDishImage(String dishName) {
        // Strategy 1: Bing Images — cleaner DOM, less anti-scraping than Google
        String url = searchBingImages(dishName);
        if (url != null) return url;

        // Strategy 2: Wikipedia Vietnamese
        url = searchWikipedia(dishName, "vi");
        if (url != null) return url;

        // Strategy 3: Wikipedia English
        url = searchWikipedia(dishName, "en");
        if (url != null) return url;

        log.warn("No image found for '{}' from any source", dishName);
        return null;
    }

    /**
     * Search Bing Images — Bing embeds image data as JSON with "murl" field.
     * Much more reliable than Google for simple HTTP scraping.
     */
    private String searchBingImages(String dishName) {
        try {
            String query = dishName + " món ăn Việt Nam";
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

            String searchUrl = "https://www.bing.com/images/search?q=" + encodedQuery
                    + "&form=HDRSC2&first=1";

            Request request = new Request.Builder()
                    .url(searchUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Accept", "text/html,application/xhtml+xml")
                    .addHeader("Accept-Language", "vi-VN,vi;q=0.9,en;q=0.8")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String html = response.body().string();
                    String imageUrl = extractBingImageUrl(html);
                    if (imageUrl != null) {
                        log.info("Bing image for '{}': {}", dishName, imageUrl);
                        return imageUrl;
                    }
                    log.debug("Bing returned HTML but no image URLs found for '{}'", dishName);
                }
            }
        } catch (Exception e) {
            log.debug("Bing image search failed for '{}': {}", dishName, e.getMessage());
        }
        return null;
    }

    /**
     * Extract image URL from Bing Images HTML.
     * Bing stores full-resolution image URLs in JSON with "murl":"URL" key.
     */
    private String extractBingImageUrl(String html) {
        // Pattern 1: Bing embeds image data as JSON with "murl":"URL"
        Pattern murlPattern = Pattern.compile("\"murl\":\"(https?://[^\"]+)\"");
        Matcher murlMatcher = murlPattern.matcher(html);

        while (murlMatcher.find()) {
            String url = murlMatcher.group(1);
            url = url.replace("\\/", "/");
            if (!url.contains("bing.com") && !url.contains("microsoft.com")
                    && !url.contains("logo") && !url.contains("icon")
                    && url.length() > 30) {
                return url;
            }
        }

        // Pattern 2: Bing thumbnail URLs in data-src attributes
        Pattern dataSrcPattern = Pattern.compile("data-src=\"(https?://tse[0-9]*\\.mm\\.bing\\.net/[^\"]+)\"");
        Matcher dataSrcMatcher = dataSrcPattern.matcher(html);

        if (dataSrcMatcher.find()) {
            return dataSrcMatcher.group(1);
        }

        // Pattern 3: Any th.bing.com thumbnail
        Pattern thumbPattern = Pattern.compile("src=\"(https?://th\\.bing\\.com/th/[^\"]+)\"");
        Matcher thumbMatcher = thumbPattern.matcher(html);

        if (thumbMatcher.find()) {
            return thumbMatcher.group(1);
        }

        return null;
    }

    /**
     * Search Wikipedia for dish image using REST API.
     */
    private String searchWikipedia(String dishName, String lang) {
        try {
            String encodedQuery = URLEncoder.encode(dishName, StandardCharsets.UTF_8.toString());

            // Direct page summary lookup
            String summaryUrl = "https://" + lang + ".wikipedia.org/api/rest_v1/page/summary/"
                    + encodedQuery;

            Request request = new Request.Builder()
                    .url(summaryUrl)
                    .addHeader("User-Agent", "GoMarket/1.0 (dichoho@example.com)")
                    .addHeader("Accept", "application/json")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("thumbnail")) {
                        String imageUrl = json.getAsJsonObject("thumbnail")
                                .get("source").getAsString();
                        log.info("Wikipedia ({}) image for '{}': {}", lang, dishName, imageUrl);
                        return imageUrl;
                    }
                }
            }

            // Fallback: search Wikipedia for related article
            String searchApiUrl = "https://" + lang + ".wikipedia.org/w/api.php"
                    + "?action=query&list=search&srsearch="
                    + URLEncoder.encode(dishName + " món ăn", StandardCharsets.UTF_8.toString())
                    + "&srnamespace=0&srlimit=3&format=json";

            Request searchReq = new Request.Builder()
                    .url(searchApiUrl)
                    .addHeader("User-Agent", "GoMarket/1.0 (dichoho@example.com)")
                    .get()
                    .build();

            try (Response searchResp = client.newCall(searchReq).execute()) {
                if (searchResp.isSuccessful() && searchResp.body() != null) {
                    String searchBody = searchResp.body().string();
                    JsonObject searchJson = JsonParser.parseString(searchBody).getAsJsonObject();

                    if (searchJson.has("query")) {
                        JsonArray results = searchJson.getAsJsonObject("query")
                                .getAsJsonArray("search");
                        for (int i = 0; i < results.size(); i++) {
                            String title = results.get(i).getAsJsonObject()
                                    .get("title").getAsString();
                            String encodedTitle = URLEncoder.encode(title,
                                    StandardCharsets.UTF_8.toString());
                            String pageSummaryUrl = "https://" + lang
                                    + ".wikipedia.org/api/rest_v1/page/summary/"
                                    + encodedTitle;

                            Request summaryReq = new Request.Builder()
                                    .url(pageSummaryUrl)
                                    .addHeader("User-Agent", "GoMarket/1.0 (dichoho@example.com)")
                                    .addHeader("Accept", "application/json")
                                    .get()
                                    .build();

                            try (Response summaryResp = client.newCall(summaryReq).execute()) {
                                if (summaryResp.isSuccessful() && summaryResp.body() != null) {
                                    String summaryBody = summaryResp.body().string();
                                    JsonObject summaryJson = JsonParser.parseString(summaryBody)
                                            .getAsJsonObject();
                                    if (summaryJson.has("thumbnail")) {
                                        String imageUrl = summaryJson
                                                .getAsJsonObject("thumbnail")
                                                .get("source").getAsString();
                                        log.info("Wikipedia search ({}) image for '{}': {}",
                                                lang, dishName, imageUrl);
                                        return imageUrl;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Wikipedia ({}) search failed for '{}': {}", lang, dishName, e.getMessage());
        }
        return null;
    }
}
