package com.origin.launcher.auth.storage;

import android.util.Base64;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.origin.launcher.auth.AuthConfig;
import com.origin.launcher.JsonIOUtils;

import coelho.msftauth.api.oauth20.OAuth20Token;

public class MsaTokenStore implements XalJsonExportable {

    private static final Gson GSON = new Gson();

    private final String json;

    private MsaTokenStore(String json) {
        this.json = json;
    }

    public static void save(Context ctx, OAuth20Token token) {
        JsonObject root = new JsonObject();
        root.addProperty("user_id", token.getUserId());
        root.addProperty("refresh_token", token.getRefreshToken());
        String foci = token.getFoci();
        root.addProperty("foci", foci != null ? foci : "");

        JsonObject at = new JsonObject();
        String atValue = token.getAccessToken() != null ? ("t=" + token.getAccessToken()) : "t=";
        at.addProperty("access_token", atValue);
        long expiresMillis = System.currentTimeMillis() + token.getExpiresIn() * 1000L;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        at.addProperty("xal_expires", sdf.format(new Date(expiresMillis)));

        String scope = token.getScope();
        String xblScope;
        if (scope != null && scope.contains("service::user.auth.xboxlive.com::")) {
            xblScope = scope.replace("MBI_SSL", "mbi_ssl").replace("offline_access", "").trim();
        } else {
            xblScope = "service::user.auth.xboxlive.com::mbi_ssl";
        }
        at.addProperty("scopes", xblScope);

        JsonArray ats = new JsonArray();
        ats.add(at);
        root.add("access_tokens", ats);

        String json = root.toString();
        MsaTokenStore s = new MsaTokenStore(json);
        File f = XalStorageManager.getMsaTokenFile(ctx, token.getUserId());
        JsonIOUtils.write(f, s.json);
    }

    @Override
    public String filename() {
        String tid = AuthConfig.productionRetailJwtDefault().defaultTitleTid();
        String userId = "";
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            userId = obj.get("user_id").getAsString();
        } catch (Exception ignored) {}
        String b64 = Base64.encodeToString(userId.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        return "Xal." + tid + ".Production.Msa." + b64 + ".json";
    }

    @Override
    public String toJson() {
        return json;
    }

    public static String findRefreshToken(Context ctx, String userId) {
        try {
            File f = XalStorageManager.getMsaTokenFile(ctx, userId);
            if (!f.exists()) return null;
            String json = JsonIOUtils.read(f);
            if (json == null) return null;
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("refresh_token") && !obj.get("refresh_token").isJsonNull()) {
                return obj.get("refresh_token").getAsString();
            }
        } catch (Exception ex) {
            android.util.Log.w("XALExport", "Failed to read MSA refresh token", ex);
        }
        return null;
    }

    public static String findRpsTicket(Context ctx, String userId) {
        try {
            File f = XalStorageManager.getMsaTokenFile(ctx, userId);
            if (!f.exists()) return null;
            String json = JsonIOUtils.read(f);
            if (json == null) return null;
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("access_tokens") && obj.get("access_tokens").isJsonArray()) {
                JsonArray ats = obj.get("access_tokens").getAsJsonArray();
                if (ats.size() > 0) {
                    JsonObject at = ats.get(0).getAsJsonObject();
                    if (at.has("access_token") && !at.get("access_token").isJsonNull()) {
                        String ticket = at.get("access_token").getAsString();
                        return (ticket != null && !ticket.trim().isEmpty()) ? ticket : null;
                    }
                }
            }
        } catch (Exception ex) {
            android.util.Log.w("XALExport", "Failed to read MSA rps ticket", ex);
        }
        return null;
    }

    public static boolean isAccessTokenValid(Context ctx, String userId) {
        try {
            File f = XalStorageManager.getMsaTokenFile(ctx, userId);
            if (!f.exists()) return false;
            String json = JsonIOUtils.read(f);
            if (json == null) return false;
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("access_tokens") || !obj.get("access_tokens").isJsonArray()) return false;
            JsonArray ats = obj.get("access_tokens").getAsJsonArray();
            if (ats.size() == 0) return false;
            JsonObject at = ats.get(0).getAsJsonObject();
            if (!at.has("access_token") || at.get("access_token").isJsonNull()) return false;
            String ticket = at.get("access_token").getAsString();
            if (ticket == null || ticket.trim().isEmpty()) return false;
            if (!at.has("xal_expires") || at.get("xal_expires").isJsonNull()) return false;
            String expiresStr = at.get("xal_expires").getAsString();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(expiresStr);
            long now = System.currentTimeMillis();
            return d != null && d.getTime() > now;
        } catch (Exception ex) {
            android.util.Log.w("XALExport", "Failed to validate MSA access token", ex);
        }
        return false;
    }
}