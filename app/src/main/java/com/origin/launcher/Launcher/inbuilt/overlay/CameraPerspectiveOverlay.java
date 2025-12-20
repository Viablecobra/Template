package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.view.KeyEvent;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

public class CameraPerspectiveOverlay extends BaseOverlayButton {

    public CameraPerspectiveOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.CAMERA_PERSPECTIVE;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_camera;
    }

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_F5);
    }
}