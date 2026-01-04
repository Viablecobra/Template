package com.origin.launcher;

import android.app.Activity;
import com.origin.launcher.LoadingDialog;

public final class DialogUtils {
    private DialogUtils() {}

    public static LoadingDialog ensure(Activity activity, LoadingDialog existing) {
        return existing != null ? existing : new LoadingDialog(activity);
    }

    public static void showWithMessage(LoadingDialog dialog, String message) {
        if (dialog == null) return;
        dialog.setMessage(message);
        if (!dialog.isShowing()) dialog.show();
    }

    public static void dismissQuietly(LoadingDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}