package com.microsoft.xboxlive;

import android.content.Context;

import org.jetbrains.annotations.NotNull;



public class LocalStorage {
    @NotNull
    public static String getPath(@NotNull Context context) {
        return context.getFilesDir().getPath();
    }
}