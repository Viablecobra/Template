package com.origin.launcher.auth.storage;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;

import coelho.msftauth.api.xbox.XboxDeviceKey;

public class DeviceIdentityStore implements XalJsonExportable {

    private static final String KEY_MARKER_VALUE = "Serialized to SharedPreferences";
    public static final String FILENAME = "Xal.Production.RETAIL.DeviceIdentity.json";
    private static final Gson GSON = new Gson();

    public static class DeviceIdentity {
        @SerializedName("Id")
        public String id;
        @SerializedName("Key")
        public String key;
    }

    private final DeviceIdentity identity;

    private DeviceIdentityStore(DeviceIdentity identity) {
        this.identity = identity;
    }

    public static void save(Context ctx, String userId, XboxDeviceKey key) {
         String id = key.getId();
         DeviceIdentity di = new DeviceIdentity();
         di.id = id;
         di.key = KEY_MARKER_VALUE;
         String json = GSON.toJson(di);
         File f = XalStorageManager.getDeviceIdentityFile(ctx, userId);
         com.origin.launcher.JsonIOUtils.write(f, json);
     }

    @Override
    public String filename() {
        return FILENAME;
    }

    @Override
    public String toJson() {
        return GSON.toJson(identity);
    }
}