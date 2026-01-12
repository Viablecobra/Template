package com.origin.launcher;

import android.content.Context;

public class FeatureSettings {
    private static volatile FeatureSettings INSTANCE;
    private static Context appContext;
    private static volatile boolean libraryLoaded = false;
    
    private boolean versionIsolationEnabled = false;
    private boolean logcatOverlayEnabled = false;
    
    private native void setAutofixVersionsNative(String[] versions);
    private native void setLightmapAutofixerNative(boolean enabled);
    private native void setTextureLodAutofixerNative(boolean enabled);

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
            setAutofixVersionsNative(versions);
        }
    }

    public void setLightmapAutofixerSafe(boolean enabled) {
        ensureLibraryLoaded();
        if (libraryLoaded) {
            setLightmapAutofixerNative(enabled);
        }
    }

    public void setTextureLodAutofixerSafe(boolean enabled) {
        ensureLibraryLoaded();
        if (libraryLoaded) {
            setTextureLodAutofixerNative(enabled);
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