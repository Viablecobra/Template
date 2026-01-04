package com.origin.launcher;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class JsonIOUtils {

    private static final String TAG = "JsonIO";

    private JsonIOUtils() {}

    public static String read(File f) {
        if (f == null || !f.exists()) return null;
        try (FileInputStream fis = new FileInputStream(f);
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            Log.w(TAG, "Failed to read " + f.getAbsolutePath(), ex);
            return null;
        }
    }

    public static boolean write(File f, String content) {
        if (f == null) return false;
        try (FileOutputStream fos = new FileOutputStream(f, false);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            osw.write(content != null ? content : "");
            return true;
        } catch (Exception ex) {
            Log.w(TAG, "Failed to write " + (f != null ? f.getAbsolutePath() : "<null>"), ex);
            return false;
        }
    }
}