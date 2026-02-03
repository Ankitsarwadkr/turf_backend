package com.example.turf_Backend.util;




import org.apache.commons.codec.digest.DigestUtils;

import java.security.SecureRandom;
import java.util.Base64;

public class ResetTokenUtil {
    private static final SecureRandom random = new SecureRandom();

    private ResetTokenUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateRawToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public static String hash(String raw) {
        return DigestUtils.sha256Hex(raw);
    }
}
