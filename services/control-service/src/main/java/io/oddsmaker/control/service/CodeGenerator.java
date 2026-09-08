package io.oddsmaker.control.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 兑换码生成器（纯逻辑，可单测）。
 * 字符集去除易混淆字符（0/O/1/I/L），SecureRandom 随机，批内去重。
 */
public final class CodeGenerator {

    /** Crockford Base32 子集：无 0/O/1/I/L */
    public static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {}

    /**
     * 批量生成 count 个长度 length 的不重复兑换码。
     *
     * @param prefix 码前缀（可为空），如 "NY26-"
     */
    public static List<String> generate(int count, int length, String prefix) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        if (length < 6 || length > 32) {
            throw new IllegalArgumentException("code length must be between 6 and 32");
        }
        String p = prefix == null ? "" : prefix;
        Set<String> seen = new HashSet<>(count * 2);
        List<String> out = new ArrayList<>(count);
        while (out.size() < count) {
            StringBuilder sb = new StringBuilder(p.length() + length);
            sb.append(p);
            for (int i = 0; i < length; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            if (seen.add(sb.toString())) {
                out.add(sb.toString());
            }
        }
        return out;
    }
}
