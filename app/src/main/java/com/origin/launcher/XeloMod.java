package com.origin.launcher;

public class XeloMod {
    private static boolean libraryLoaded = false;

    public static synchronized boolean loadLibrary() {
        if (libraryLoaded) return true;
        try {
            System.loadLibrary("mtbinloader2");
            libraryLoaded = true;
            return true;
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isLoaded() {
        return libraryLoaded;
    }
}