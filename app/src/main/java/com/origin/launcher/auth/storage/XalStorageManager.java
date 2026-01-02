package com.origin.launcher.auth.storage;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.os.Environment;

import com.google.gson.Gson;

import com.origin.launcher.auth.AuthConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import coelho.msftauth.api.xbox.XboxDeviceKey;
import coelho.msftauth.api.xbox.XboxDeviceToken;
import coelho.msftauth.api.xbox.XboxTitleToken;

public class XalStorageManager {

    private static final Gson GSON = new Gson();

    public static void saveDefaultTitleUser(Context ctx, String msUserId) {
        TitleDefaultStore.save(ctx, msUserId);
    }

    public static Map<String, String> exportAll(Context ctx) {
        Map<String, String> out = new HashMap<>();
        File dir = getXalDir(ctx);
        collectJsonFilesRecursively(dir, out);
        return out;
    }

    private static void collectJsonFilesRecursively(File dir, Map<String, String> out) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectJsonFilesRecursively(f, out);
            } else if (f.getName().endsWith(".json")) {
                try (FileInputStream fis = new FileInputStream(f);
                     java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = fis.read(buf)) != -1) {
                        bos.write(buf, 0, read);
                    }
                    String json = bos.toString(StandardCharsets.UTF_8.name());
                    out.put(f.getAbsolutePath(), json);
                } catch (Exception ex) {
                    Log.w("XALExport", "Failed to read " + f.getAbsolutePath(), ex);
                }
            }
        }
    }

    public static File getXalDir(Context ctx) {
        File dir = new File(ctx.getApplicationContext().getFilesDir(), "xal");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getUserDir(Context ctx, String userId) {
        File root = getXalDir(ctx);
        String b64 = Base64.encodeToString(
                userId.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
        );
        File userDir = new File(root, b64);
        if (!userDir.exists()) {
            try {
                userDir.mkdirs();
            } catch (Exception e) {
                Log.w("XALStorage", "Failed to create user dir: " + userDir.getAbsolutePath(), e);
            }
        }
        return userDir;
    }

    public static boolean deleteUserDir(Context ctx, String userId) {
        if (userId == null || userId.isEmpty()) return false;
        File root = getXalDir(ctx);
        String b64 = Base64.encodeToString(
                userId.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
        );
        File userDir = new File(root, b64);
        return deleteDirectory(userDir);
    }

    private static boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return false;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    try {
                        file.delete();
                    } catch (Exception ignored) {}
                }
            }
        }
        try {
            return dir.delete();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void saveDeviceIdentity(Context ctx, String userId, XboxDeviceKey deviceKey) {
        deviceKey.storeKeyPairAndId(ctx);
        DeviceIdentityStore.save(ctx, userId, deviceKey);
    }

    public static void saveDeviceToken(Context ctx, String userId, XboxDeviceKey deviceKey, XboxDeviceToken deviceToken, AuthConfig config) {
        DTokenStore.save(ctx, userId, deviceKey, deviceToken, config);
    }

    public static void saveTitleToken(Context ctx, String userId, XboxDeviceKey deviceKey, XboxTitleToken titleToken, AuthConfig config) {
        TTokenStore.save(ctx, userId, deviceKey, titleToken, config);
    }

    public static void writeAllToFiles(Context ctx) {
        Map<String, String> exports = exportAll(ctx);
        for (Map.Entry<String, String> e : exports.entrySet()) {
            File f = new File(e.getKey());
            try (FileOutputStream fos = new FileOutputStream(f, false);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                osw.write(e.getValue());
            } catch (Exception ex) {
                Log.w("XALExport", "Failed to write " + f.getAbsolutePath(), ex);
            }
        }
    }

    public static String base64UserId(String userId) {
        return Base64.encodeToString(
                userId.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
        );
    }

    public static File getMsaTokenFile(Context ctx, String userId) {
        String tid = com.origin.launcher.auth.AuthConfig.productionRetailJwtDefault().defaultTitleTid();
        String b64 = base64UserId(userId);
        File dir = getUserDir(ctx, userId);
        return new File(dir, "Xal." + tid + ".Production.Msa." + b64 + ".json");
    }

    public static File getDeviceIdentityFile(Context ctx, String userId) {
        File dir = getUserDir(ctx, userId);
        return new File(dir, DeviceIdentityStore.FILENAME);
    }

    public static File getTitleDefaultFile(Context ctx, String userId) {
        String tid = com.origin.launcher.auth.AuthConfig.productionRetailJwtDefault().defaultTitleTid();
        File dir = getUserDir(ctx, userId);
        return new File(dir, "Xal." + tid + ".Production.Default.json");
    }

    public static File getUserTokenFile(Context ctx, String userId) {
        String tid = com.origin.launcher.auth.AuthConfig.productionRetailJwtDefault().defaultTitleTid();
        String b64 = base64UserId(userId);
        File dir = getUserDir(ctx, userId);
        return new File(dir, "Xal." + tid + ".Production.RETAIL.User." + b64 + ".json");
    }
}