package com.origin.launcher;

import android.content.Context;

import com.origin.launcher.R;
import com.origin.launcher.auth.MsftAccountStore;

public final class AccountTextUtils {
    private AccountTextUtils() {}

    public static String titleOrUnknown(MsftAccountStore.MsftAccount a) {
        if (a == null) return "Unknown";
        return a.minecraftUsername != null ? a.minecraftUsername : (a.xboxGamertag != null ? a.xboxGamertag : "Unknown");
    }

    public static String subtitle(MsftAccountStore.MsftAccount a) {
        if (a == null) return "";
        if (a.xuid != null && !a.xuid.isEmpty()) return "XUID: " + a.xuid;
        if (a.msUserId != null && !a.msUserId.isEmpty()) return "MS ID: " + a.msUserId;
        return "";
    }

    public static String displayNameOrNotSigned(Context ctx, MsftAccountStore.MsftAccount a) {
        if (a == null) return ctx.getString(R.string.not_signed_in);
        String name = a.minecraftUsername != null ? a.minecraftUsername : (a.xboxGamertag != null ? a.xboxGamertag : null);
        return name != null ? name : ctx.getString(R.string.not_signed_in);
    }

    public static boolean isRecentlyUpdated(MsftAccountStore.MsftAccount a, int days) {
        if (a == null) return false;
        long lastUpdated = a.lastUpdated;
        if (lastUpdated <= 0L) return false;
        long daysMillis = days * 24L * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();
        return (now - lastUpdated) < daysMillis;
    }

    public static String sanitizeUrl(String url) {
        if (url == null) return null;
        String u = url.replace("`", "").trim();
        if (!(u.startsWith("http://") || u.startsWith("https://"))) return null;
        return u;
    }
}