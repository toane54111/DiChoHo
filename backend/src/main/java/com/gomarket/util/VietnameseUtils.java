package com.gomarket.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility xử lý tiếng Việt: bỏ dấu, normalize cho search
 *
 * VD: "Thịt bò Úc" → "thit bo uc"
 *     "Cá & Hải sản" → "ca & hai san"
 */
public class VietnameseUtils {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Bỏ dấu tiếng Việt + lowercase
     * "Thịt bò Úc" → "thit bo uc"
     */
    public static String removeDiacritics(String text) {
        if (text == null) return "";
        // Đ/đ không được xử lý bởi Normalizer → replace thủ công
        String result = text.replace('Đ', 'D').replace('đ', 'd');
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = DIACRITICS.matcher(result).replaceAll("");
        return result.toLowerCase().trim();
    }

    /**
     * Kiểm tra text có chứa query không (so sánh không dấu)
     * contains("Thịt bò Úc", "thit bo") → true
     * contains("Cà chua", "ca") → true
     */
    public static boolean containsIgnoreDiacritics(String text, String query) {
        return removeDiacritics(text).contains(removeDiacritics(query));
    }
}
