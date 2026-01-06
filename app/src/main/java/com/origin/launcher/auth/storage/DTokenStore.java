package com.origin.launcher.auth.storage;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;

import com.origin.launcher.auth.AuthConfig;

import coelho.msftauth.api.xbox.XboxDeviceKey;
import coelho.msftauth.api.xbox.XboxDeviceToken;

public class DTokenStore implements XalJsonExportable {

    public static final String FILENAME = "Xal.Production.RETAIL.D.json";
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

    private DTokenStore(TokenBox box) {
        this.box = box;
    }

    public static void save(Context ctx, String userId, XboxDeviceKey deviceKey, XboxDeviceToken deviceToken, AuthConfig config) {
        TokenEntry entry = new TokenEntry();
        entry.identityType = "xal.devicetoken";
        entry.environment = config.environment();
        entry.relyingParty = config.deviceAuthRP();
        TokenData td = new TokenData();
        td.oauth20Token = deviceToken.getToken();
        entry.tokenData = td;

        TokenBox box = new TokenBox();
        box.deviceId = deviceKey.getId();
        box.tokens = new TokenEntry[]{entry};
        String json = GSON.toJson(box);

        java.io.File dir = XalStorageManager.getUserDir(ctx, userId);
        java.io.File f = new java.io.File(dir, FILENAME);
        com.origin.launcher.JsonIOUtils.write(f, json);
    }


    @Override
    public String filename() {
        return FILENAME;
    }

    @Override
    public String toJson() {
        return GSON.toJson(box);
    }
}