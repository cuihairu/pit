package io.oddsmaker.control.experiment;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 实验分流器：确定性哈希分桶 + 变体权重分配。
 *
 * 算法：SHA-256(salt + ":" + subjectId) 取前 4 字节 → [0, 10000) 桶，
 * 按变体累积权重落位。同一 (salt, subjectId) 永远落在同一变体，
 * 保证曝光/转化口径稳定；SDK 端实现相同算法即可与服务端一致。
 */
public final class ExperimentSplitter {

    public static final int BUCKETS = 10_000;

    /** 变体分配：name + weight（正整数） */
    public static final class Variant {
        public final String name;
        public final int weight;

        public Variant(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }
    }

    private ExperimentSplitter() {}

    /**
     * 为主体（userId/deviceId）分配变体。
     *
     * @param salt 实验盐值（保证不同实验之间分配独立）
     * @param subjectId 主体标识
     * @param variants 变体列表（weight 之和需大于 0）
     * @return 命中的变体名；无有效变体时返回 null
     */
    public static String assign(String salt, String subjectId, List<Variant> variants) {
        if (salt == null || subjectId == null || variants == null || variants.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (Variant v : variants) {
            if (v.weight <= 0) {
                return null;
            }
            totalWeight += v.weight;
        }

        int bucket = bucketOf(salt, subjectId);
        int cumulative = 0;
        for (Variant v : variants) {
            cumulative += v.weight;
            if (bucket < cumulative * BUCKETS / totalWeight) {
                return v.name;
            }
        }
        // 整除边界兜底：落在最后一个变体
        return variants.get(variants.size() - 1).name;
    }

    /** 从实验配置 JSON 解析变体列表 */
    public static List<Variant> parseVariants(JsonNode config) {
        List<Variant> out = new ArrayList<>();
        if (config == null || !config.has("variants")) {
            return out;
        }
        JsonNode variants = config.get("variants");
        if (!variants.isArray()) {
            return out;
        }
        for (JsonNode variant : variants) {
            if (variant == null || !variant.isObject()) continue;
            JsonNode name = variant.get("name");
            JsonNode weight = variant.get("weight");
            if (name == null || !name.isTextual() || name.asText().isBlank()) continue;
            int w = weight != null && weight.isIntegralNumber() ? weight.asInt() : 0;
            if (w <= 0) continue;
            out.add(new Variant(name.asText().trim(), w));
        }
        return out;
    }

    /** 主体落桶：SHA-256 摘要前 4 字节无符号整数取模 */
    static int bucketOf(String salt, String subjectId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest((salt + ":" + subjectId).getBytes(StandardCharsets.UTF_8));
            int value = ((digest[0] & 0xFF) << 24)
                | ((digest[1] & 0xFF) << 16)
                | ((digest[2] & 0xFF) << 8)
                | (digest[3] & 0xFF);
            return Math.floorMod(value, BUCKETS);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 为 JVM 必备算法
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
