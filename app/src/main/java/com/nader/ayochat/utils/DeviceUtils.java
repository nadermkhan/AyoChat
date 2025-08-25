package com.nader.ayochat.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import java.security.MessageDigest;
import java.util.UUID;

public class DeviceUtils {

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        if (androidId == null || androidId.isEmpty()) {
            androidId = UUID.randomUUID().toString();
        }

        // Generate a 25-character device ID
        return generateDeviceId(androidId);
    }

    private static String generateDeviceId(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            // Take first 25 characters and format like a product key
            String raw = hexString.toString().toUpperCase().substring(0, 25);
            return String.format("%s-%s-%s-%s-%s",
                    raw.substring(0, 5),
                    raw.substring(5, 10),
                    raw.substring(10, 15),
                    raw.substring(15, 20),
                    raw.substring(20, 25)
            );
        } catch (Exception e) {
            // Fallback to a simple UUID-based ID
            String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            return String.format("%s-%s-%s-%s-%s",
                    uuid.substring(0, 5),
                    uuid.substring(5, 10),
                    uuid.substring(10, 15),
                    uuid.substring(15, 20),
                    uuid.substring(20, 25)
            );
        }
    }
}