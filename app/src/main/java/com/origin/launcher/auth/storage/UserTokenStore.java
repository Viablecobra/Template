package com.origin.launcher.auth.storage;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import com.origin.launcher.auth.AuthConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;

import android.util.Base64;
import coelho.msftauth.api.xbox.XboxToken;
import coelho.msftauth.api.xbox.XboxDeviceKey;

public class UserTokenStore implements XalJsonExportable {

    private static final Gson GSON = new Gson();

    private final String json;

    private UserTokenStore(String json) {
        this.json = json;
    }

    static class XuiClaims {
        @SerializedName("uhs") public String uhs = "";
        @SerializedName("gtg") public String gtg = "";
        @SerializedName("mgt") public String mgt = "";
        @SerializedName("mgs") public String mgs = "";
        @SerializedName("umg") public String umg = "";
        @SerializedName("xid") public String xid = "0";
        @SerializedName("agg") public String agg = "";
        @SerializedName("prv") public String prv = "";
        @SerializedName("usr") public String usr = "";
        @SerializedName("uer") public String uer = "";
        @SerializedName("utr") public String utr = "";
    }

    static class TokenDisplayClaims {
        @SerializedName("xui") public List<XuiClaims> xui;
    }

    static class TokenData {
        @SerializedName("Token") public String token;
        @SerializedName("NotAfter") public String notAfter;
        @SerializedName("IssueInstant") public String issueInstant;
        @SerializedName("ClientAttested") public boolean clientAttested;
        @SerializedName("DisplayClaims") public TokenDisplayClaims displayClaims;
    }

    static class TokenEnvelope {
        @SerializedName("MsaUserId") public String msaUserId;
        @SerializedName("HasSignInDisplayClaims") public boolean hasSignInDisplayClaims;
        @SerializedName("IdentityType") public String identityType;
        @SerializedName("Environment") public String environment;
        @SerializedName("Sandbox") public String sandbox;
        @SerializedName("TokenType") public String tokenType;
        @SerializedName("RelyingParty") public String relyingParty;
        @SerializedName("SubRelyingParty") public String subRelyingParty;
        @SerializedName("TokenData") public TokenData tokenData;
    }

    static class UJson {
        @SerializedName("deviceId") public String deviceId;
        @SerializedName("tokens") public List<TokenEnvelope> tokens;
    }

    private static XuiClaims toXuiClaims(XboxToken tok) {
        XuiClaims x = new XuiClaims();
        if (tok != null && tok.getDisplayClaims() != null) {
            JsonElement xuiEl = tok.getDisplayClaims().get("xui");
            if (xuiEl != null && xuiEl.isJsonArray() && xuiEl.getAsJsonArray().size() > 0) {
                JsonObject obj = xuiEl.getAsJsonArray().get(0).getAsJsonObject();
                if (obj.has("uhs") && !obj.get("uhs").isJsonNull()) x.uhs = obj.get("uhs").getAsString();
                if (obj.has("gtg") && !obj.get("gtg").isJsonNull()) x.gtg = obj.get("gtg").getAsString();
                if (obj.has("mgt") && !obj.get("mgt").isJsonNull()) x.mgt = obj.get("mgt").getAsString();
                if (obj.has("mgs") && !obj.get("mgs").isJsonNull()) x.mgs = obj.get("mgs").getAsString();
                if (obj.has("umg") && !obj.get("umg").isJsonNull()) x.umg = obj.get("umg").getAsString();
                if (obj.has("xid") && !obj.get("xid").isJsonNull()) x.xid = obj.get("xid").getAsString();
                if (obj.has("agg") && !obj.get("agg").isJsonNull()) x.agg = obj.get("agg").getAsString();
                if (obj.has("prv") && !obj.get("prv").isJsonNull()) x.prv = obj.get("prv").getAsString();
                if (obj.has("usr") && !obj.get("usr").isJsonNull()) x.usr = obj.get("usr").getAsString();
                if (obj.has("uer") && !obj.get("uer").isJsonNull()) x.uer = obj.get("uer").getAsString();
                if (obj.has("utr") && !obj.get("utr").isJsonNull()) x.utr = obj.get("utr").getAsString();
            }
        }
        return x;
    }

    private static TokenEnvelope buildEnvelope(String identityType, String relyingParty, String msaUserId, AuthConfig config, XboxToken tok) {
        TokenDisplayClaims displayClaims = new TokenDisplayClaims();
        displayClaims.xui = Collections.singletonList(toXuiClaims(tok));

        TokenData data = new TokenData();
        data.token = tok != null && tok.getToken() != null ? tok.getToken() : "";
        data.notAfter = tok != null && tok.getNotAfter() != null ? tok.getNotAfter() : "";
        data.issueInstant = tok != null && tok.getIssueInstant() != null ? tok.getIssueInstant() : "";
        data.clientAttested = false;
        data.displayClaims = displayClaims;

        TokenEnvelope env = new TokenEnvelope();
        env.msaUserId = msaUserId;
        env.hasSignInDisplayClaims = displayClaims.xui != null && !displayClaims.xui.isEmpty() && displayClaims.xui.get(0).gtg != null && !displayClaims.xui.get(0).gtg.isEmpty();
        env.identityType = identityType;
        env.environment = (config != null ? config.environment() : "Production");
        env.sandbox = (config != null ? config.sandbox() : "RETAIL");
        env.tokenType = (config != null ? config.tokenType() : "JWT");
        env.relyingParty = relyingParty;
        env.subRelyingParty = "";
        env.tokenData = data;
        return env;
    }

    public static void save(Context ctx, XboxDeviceKey deviceKey, String msaUserId, AuthConfig config,
                             XboxToken uToken, XboxToken xstsXboxLive, XboxToken xstsPlayfab, XboxToken xstsRealms) {
        List<TokenEnvelope> envs = new ArrayList<>();
        // XSTS tokens
        envs.add(buildEnvelope("Xtoken", "http://xboxlive.com", msaUserId, config, xstsXboxLive));
        envs.add(buildEnvelope("Xtoken", "https://b980a380.minecraft.playfabapi.com/", msaUserId, config, xstsPlayfab));
        envs.add(buildEnvelope("Xtoken", "https://pocket.realms.minecraft.net/", msaUserId, config, xstsRealms));
        // Utoken (user token)
        envs.add(buildEnvelope("Utoken", (config != null ? config.userAuthRP() : "http://auth.xboxlive.com"), msaUserId, config, uToken));

        UJson root = new UJson();
        root.deviceId = deviceKey.getId();
        root.tokens = envs;

        String json = GSON.toJson(root);
        File f = XalStorageManager.getUserTokenFile(ctx, msaUserId);
        com.origin.launcher.JsonIOUtils.write(f, json);
    }

    @Override
    public String filename() {
        String tid = AuthConfig.productionRetailJwtDefault().defaultTitleTid();
        String userId = "";
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            JsonArray tokens = obj.getAsJsonArray("tokens");
            if (tokens != null && tokens.size() > 0) {
                JsonObject env = tokens.get(0).getAsJsonObject();
                if (env.has("MsaUserId") && !env.get("MsaUserId").isJsonNull()) {
                    userId = env.get("MsaUserId").getAsString();
                }
            }
        } catch (Exception ignored) {}
        String b64 = Base64.encodeToString(userId.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        return "Xal." + tid + ".Production.RETAIL.User." + b64 + ".json";
    }

    @Override
    public String toJson() {
        return json;
    }
}