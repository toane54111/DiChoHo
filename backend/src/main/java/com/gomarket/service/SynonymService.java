package com.gomarket.service;

import com.gomarket.util.VietnameseUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Query Rewrite - Xử lý từ đồng nghĩa vùng miền Việt Nam
 *
 * Data BHX dùng tiếng miền Nam → cần dịch từ Bắc/Trung sang Nam.
 * Hỗ trợ: synonym vùng miền, viết tắt, tên gọi khác nhau.
 *
 * Flow: "thịt lợn xay" → rewrite → "thịt heo xay"
 *       "quả dứa"      → rewrite → "trái thơm"
 *       "rau muống"     → giữ nguyên (đã chuẩn)
 *
 * Trả về List<String> gồm query gốc + các biến thể để search tất cả.
 */
@Service
public class SynonymService {

    // LinkedHashMap giữ thứ tự: cụm dài check trước cụm ngắn
    // (tránh "thịt lợn" bị thay "lợn" trước khi match cả cụm)
    private final Map<String, String> synonyms = new LinkedHashMap<>();

    public SynonymService() {
        // ═══════════════════════════════════════════
        // THỊT - Bắc gọi "lợn", Nam gọi "heo"
        // ═══════════════════════════════════════════
        synonyms.put("thịt lợn", "thịt heo");
        synonyms.put("sườn lợn", "sườn heo");
        synonyms.put("ba chỉ lợn", "ba rọi heo");
        synonyms.put("ba chỉ", "ba rọi");
        synonyms.put("nạc vai lợn", "nạc vai heo");
        synonyms.put("giò lợn", "giò heo");
        synonyms.put("chân giò lợn", "chân giò heo");
        synonyms.put("móng lợn", "móng heo");
        synonyms.put("lợn", "heo");

        // ═══════════════════════════════════════════
        // TRÁI CÂY - Bắc vs Nam
        // ═══════════════════════════════════════════
        synonyms.put("quả dứa", "trái thơm");
        synonyms.put("trái dứa", "trái thơm");
        synonyms.put("dứa", "thơm");
        synonyms.put("quả na", "mãng cầu");
        synonyms.put("trái na", "mãng cầu");
        synonyms.put("na", "mãng cầu");
        synonyms.put("quả vải", "trái vải");
        synonyms.put("quả ổi", "trái ổi");
        synonyms.put("quả bưởi", "trái bưởi");
        synonyms.put("quả cam", "trái cam");
        synonyms.put("quả chanh", "trái chanh");
        synonyms.put("quả táo", "trái táo");
        synonyms.put("quả lê", "trái lê");
        synonyms.put("quả chuối", "trái chuối");

        // ═══════════════════════════════════════════
        // RAU CỦ - Bắc vs Nam
        // ═══════════════════════════════════════════
        synonyms.put("củ sắn", "khoai mì");      // Bắc: sắn = Nam: khoai mì
        synonyms.put("sắn", "khoai mì");
        synonyms.put("rau mùi", "rau ngò");       // Bắc: mùi = Nam: ngò
        synonyms.put("mùi tàu", "ngò gai");
        synonyms.put("thì là", "thìa là");
        synonyms.put("su hào", "su hào");          // giống nhau nhưng BHX có thể gọi khác
        synonyms.put("củ đậu", "củ sắn dây");

        // ═══════════════════════════════════════════
        // HẠT / GIA VỊ
        // ═══════════════════════════════════════════
        synonyms.put("đậu phộng", "đậu phộng");   // Nam dùng đậu phộng, BHX cũng vậy
        synonyms.put("lạc", "đậu phộng");          // Bắc: lạc = Nam: đậu phộng
        synonyms.put("vừng", "mè");                // Bắc: vừng = Nam: mè
        synonyms.put("vừng đen", "mè đen");
        synonyms.put("vừng trắng", "mè trắng");

        // ═══════════════════════════════════════════
        // HẢI SẢN
        // ═══════════════════════════════════════════
        synonyms.put("con tôm", "tôm");
        synonyms.put("con cá", "cá");
        synonyms.put("con mực", "mực");
        synonyms.put("con cua", "cua");
        synonyms.put("con ghẹ", "ghẹ");

        // ═══════════════════════════════════════════
        // MÌ / BÚN / PHỞ
        // ═══════════════════════════════════════════
        synonyms.put("mì tôm", "mì gói");
        synonyms.put("mì ăn liền", "mì gói");
        synonyms.put("miến dong", "miến");
        synonyms.put("bún khô", "bún");
        synonyms.put("phở khô", "phở");
        synonyms.put("cháo gói", "cháo");

        // ═══════════════════════════════════════════
        // NƯỚC CHẤM / GIA VỊ
        // ═══════════════════════════════════════════
        synonyms.put("xì dầu", "nước tương");     // Bắc: xì dầu = Nam: nước tương
        synonyms.put("tương ớt", "tương ớt");
        synonyms.put("nước mắm", "nước mắm");
        synonyms.put("dầu hào", "dầu hào");
        synonyms.put("bột ngọt", "bột ngọt");
        synonyms.put("mì chính", "bột ngọt");     // Bắc: mì chính = Nam: bột ngọt
        synonyms.put("hạt nêm", "hạt nêm");

        // ═══════════════════════════════════════════
        // KHÁC
        // ═══════════════════════════════════════════
        synonyms.put("đậu hũ", "đậu hũ");
        synonyms.put("đậu phụ", "đậu hũ");        // Bắc: đậu phụ = Nam: đậu hũ
        synonyms.put("tàu hũ", "đậu hũ");
        synonyms.put("sữa chua", "sữa chua");
        synonyms.put("da ua", "sữa chua");         // tiếng lóng/viết tắt
    }

    /**
     * Rewrite query: thay thế từ vùng miền → từ chuẩn BHX
     * Hỗ trợ cả có dấu và KHÔNG DẤU
     *
     * VD: "thịt lợn xay" → "thịt heo xay"
     *     "thit lon xay"  → "thịt heo xay"  (không dấu cũng match!)
     */
    public String rewriteQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) return rawQuery;

        String result = rawQuery.toLowerCase().trim();
        String resultNoDiacritics = VietnameseUtils.removeDiacritics(result);

        // Check cả cụm trước (LinkedHashMap giữ thứ tự insert)
        for (Map.Entry<String, String> entry : synonyms.entrySet()) {
            String keyNoDiacritics = VietnameseUtils.removeDiacritics(entry.getKey());

            // Match có dấu hoặc không dấu
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            } else if (resultNoDiacritics.contains(keyNoDiacritics)) {
                // "thit lon" contains "thit lon" (normalized "thịt lợn") → replace
                resultNoDiacritics = resultNoDiacritics.replace(keyNoDiacritics,
                        VietnameseUtils.removeDiacritics(entry.getValue()));
                // Trả về bản có dấu của value thay vì bản không dấu
                result = rebuildWithDiacritics(result, entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Thay thế phần không dấu trong query bằng value có dấu
     * "thit lon xay" + key="thịt lợn" + value="thịt heo" → "thịt heo xay"
     */
    private String rebuildWithDiacritics(String query, String key, String value) {
        String queryNorm = VietnameseUtils.removeDiacritics(query);
        String keyNorm = VietnameseUtils.removeDiacritics(key);

        int idx = queryNorm.indexOf(keyNorm);
        if (idx < 0) return query;

        // Thay thế đoạn match bằng value có dấu
        String before = query.substring(0, idx);
        String after = query.substring(idx + keyNorm.length());
        return before + value + after;
    }

    /**
     * Trả về tất cả biến thể của query (gốc + đã rewrite)
     * Để search cả 2 phiên bản
     *
     * VD: "thịt lợn" → ["thịt lợn", "thịt heo"]
     *     "thit lon"  → ["thit lon", "thịt heo"]
     *     "thịt bò"  → ["thịt bò"] (không có synonym)
     */
    public List<String> getQueryVariants(String rawQuery) {
        List<String> variants = new ArrayList<>();
        String lower = rawQuery.toLowerCase().trim();
        variants.add(lower);

        String rewritten = rewriteQuery(rawQuery);
        if (!rewritten.equals(lower)) {
            variants.add(rewritten);
        }

        return variants;
    }
}
