package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.view.KeyEvent;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

public class ToggleHudOverlay extends BaseOverlayButton {

    public ToggleHudOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.TOGGLE_HUD;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_hud;
    }

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_F1);
    }
}