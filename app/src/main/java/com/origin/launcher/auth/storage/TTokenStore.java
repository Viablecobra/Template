package com.origin.launcher.auth.storage;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;

import com.origin.launcher.auth.AuthConfig;

import coelho.msftauth.api.xbox.XboxDeviceKey;
import coelho.msftauth.api.xbox.XboxTitleToken;

public class TTokenStore implements XalJsonExportable {

    private static final Gson GSON = new Gson();

    public static class TokenData {
        @SerializedName("Oauth20Token")
        public String oauth20Token;
    }

    public static class TokenEntry {
        @SerializedName("IdentityType")
        public String identityType;
        @SerializedName("Environment")
        public String environment;
        @SerializedName("RelyingParty")
        public String relyingParty;
        @SerializedName("TokenData")
        public TokenData tokenData;
    }

    public static class TokenBox {
        @SerializedName("DeviceId")
        public String deviceId;
        @SerializedName("tokens")
        public TokenEntry[] tokens;
    }

    private final TokenBox box;
    private final String filename;

    private TTokenStore(TokenBox box, String filename) {
        this.box = box;
        this.filename = filename;
    }

    public static String filename(AuthConfig config) {
        String tid = config.defaultTitleTid();
        return "Xal." + tid + ".Production.RETAIL.T.json";
    }

    public static void save(Context ctx, String userId, XboxDeviceKey deviceKey, XboxTitleToken titleToken, AuthConfig config) {
        TokenEntry entry = new TokenEntry();
        entry.identityType = "xal.titletoken";
        entry.environment = config.environment();
        entry.relyingParty = config.userAuthRP();
        TokenData td = new TokenData();
        td.oauth20Token = titleToken.getToken();
        entry.tokenData = td;

        TokenBox box = new TokenBox();
        box.deviceId = deviceKey.getId();
        box.tokens = new TokenEntry[]{entry};

        String json = GSON.toJson(box);
        java.io.File dir = XalStorageManager.getUserDir(ctx, userId);
        java.io.File f = new java.io.File(dir, filename(config));
        com.origin.launcher.JsonIOUtils.write(f, json);
    }


    @Override
    public String filename() {
        return filename;
    }

    @Override
    public String toJson() {
        return GSON.toJson(box);
    }
}