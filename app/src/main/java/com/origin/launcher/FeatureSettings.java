package com.origin.launcher;

import android.content.Context;
import android.util.Log;

public class FeatureSettings {
    private static volatile FeatureSettings INSTANCE;
    private static Context appContext;
    private static volatile boolean libraryLoaded = false;
    
    private boolean versionIsolationEnabled = false;
    private boolean logcatOverlayEnabled = false;
    
    public native void setAutofixVersions(String[] versions);
    public native void setLightmapAutofixer(boolean enabled);
    public native void setTextureLodAutofixer(boolean enabled);

    public enum StorageType {
        INTERNAL,
        EXTERNAL,
        VERSION_ISOLATION
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static FeatureSettings getInstance() {
        if (INSTANCE == null) {
            synchronized (FeatureSettings.class) {
                if (INSTANCE == null) {
                    INSTANCE = SettingsStorage.load(appContext);
                    if (INSTANCE == null) {
                        INSTANCE = new FeatureSettings();
                    }
                }
            }
        }
        return INSTANCE;
    }

    private static void ensureLibraryLoaded() {
        if (!libraryLoaded) {
            synchronized (FeatureSettings.class) {
                if (!libraryLoaded) {
                    try {
                        System.loadLibrary("mtbinloader2");
                        libraryLoaded = true;
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    public void setAutofixVersionsSafe(String[] versions) {
        ensureLibraryLoaded();
        if (libraryLoaded) {
            setAutofixVersions(versions);
        }
    }

    public void setLightmapAutofixerSafe(boolean enabled) {
        ensureLibraryLoaded();
        if (libraryLoaded) {
            setLightmapAutofixer(enabled);
        }
    }

    public void setTextureLodAutofixerSafe(boolean enabled) {
        ensureLibraryLoaded();
        if (libraryLoaded) {
            setTextureLodAutofixer(enabled);
        }
    }

    public boolean isVersionIsolationEnabled() { return versionIsolationEnabled; }
    public void setVersionIsolationEnabled(boolean enabled) { this.versionIsolationEnabled = enabled; autoSave(); }

    public boolean isLogcatOverlayEnabled() { return logcatOverlayEnabled; }
    public void setLogcatOverlayEnabled(boolean enabled) { this.logcatOverlayEnabled = enabled; autoSave(); }

    private void autoSave() {
        if (appContext != null) {
            SettingsStorage.save(appContext, this);
        }
    }
}