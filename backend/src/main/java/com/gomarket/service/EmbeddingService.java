package com.gomarket.service;

import com.google.gson.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * EmbeddingService - Chuyển text thành vector embedding dùng Ollama Halong (local)
 *
 * Flow trong RAG:
 * 1. Khi seed data: embed("Tôm sú - Hải sản") → lưu vector vào DB
 * 2. Khi user tìm: embed("Tôm sú") → so sánh cosine với vector trong DB
 *
 * Model: Halong - embedding model thuần Việt, chạy trên Ollama local
 * → Hiểu ngữ nghĩa tiếng Việt tốt, không cần prefix như nomic-embed-text
 *
 * Tối ưu:
 * - Chạy local → không bị rate limit 429 như Gemini API
 * - Cache embedding trong memory → tránh gọi lại cho cùng text
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${ollama.embedding.url:http://localhost:11434/api/embeddings}")
    private String ollamaUrl;

    @Value("${ollama.embedding.model:bge-m3}")
    private String model;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    /**
     * Cache embedding trong memory
     * Key: text đã normalize (lowercase, trim)
     * Value: float[] vector
     */
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    /**
     * Chuyển text thành vector embedding
     * Có cache: nếu text đã embed trước đó → trả từ cache
     *
     * @param text văn bản cần nhúng (VD: "Tôm sú", "Rau muống - Rau củ")
     * @return float[] vector, hoặc null nếu lỗi
     */
    public float[] embed(String text) {
        String cacheKey = text.toLowerCase().trim();

        // Kiểm tra cache trước
        float[] cached = embeddingCache.get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT: '{}' → vector[{}]", text, cached.length);
            return cached;
        }

        // Cache MISS → gọi Ollama local
        log.debug("Cache MISS: '{}' → gọi Ollama Embedding...", text);
        float[] vector = callOllamaEmbedding(text);

        if (vector != null) {
            embeddingCache.put(cacheKey, vector);
            log.debug("Embedded & cached: '{}' → vector[{}] (cache size: {})",
                    text, vector.length, embeddingCache.size());
        }

        return vector;
    }

    /**
     * Gọi Ollama Embedding API (local)
     * POST http://localhost:11434/api/embeddings
     * Body: {"model": "halong", "prompt": "text to embed"}
     * Response: {"embedding": [0.1, 0.2, ...]}
     */
    private float[] callOllamaEmbedding(String text) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("prompt", text);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(ollamaUrl)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("Ollama Embedding error: HTTP {}", response.code());
                    if (response.body() != null) {
                        log.error("Response body: {}", response.body().string());
                    }
                    return null;
                }

                String responseStr = response.body().string();
                return parseOllamaEmbedding(responseStr);
            }
        } catch (Exception e) {
            log.error("Failed to embed text '{}': {}", text, e.getMessage());
            log.error("Hãy đảm bảo Ollama đang chạy: ollama serve");
            return null;
        }
    }

    /**
     * Parse response từ Ollama Embedding API
     * Response format: { "embedding": [0.1, 0.2, ...] }
     */
    private float[] parseOllamaEmbedding(String responseJson) {
        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray embedding = root.getAsJsonArray("embedding");

        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i).getAsFloat();
        }

        log.debug("Ollama embedded text → vector[{}]", vector.length);
        return vector;
    }

    public int getCacheSize() {
        return embeddingCache.size();
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;

        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }
}
