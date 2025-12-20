package com.origin.launcher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.os.Looper;

public class SettingsFragment extends BaseThemedFragment implements DiscordManager.DiscordLoginCallback {

    private EditText packageNameEdit;
    private LinearLayout themesButton;
    private LinearLayout configurationButton;
    private LinearLayout aboutButton; 
    private LinearLayout supportButton;
    
    // Discord components
    private com.google.android.material.button.MaterialButton discordLoginButton;
    private TextView discordStatusText;
    private TextView discordUserText;
    private DiscordManager discordManager;
    
    private static final String PREF_PACKAGE_NAME = "mc_package_name";
    private static final String DEFAULT_PACKAGE_NAME = "com.mojang.minecraftpe";
    private static final String TAG = "SettingsFragment";
    private Handler mainHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        packageNameEdit = view.findViewById(R.id.mc_pkgname);
        
        // Initialize themes and about buttons
        themesButton = view.findViewById(R.id.themes_button);
        configurationButton = view.findViewById(R.id.configuration_button);
        aboutButton = view.findViewById(R.id.about_button);
        supportButton = view.findViewById(R.id.support_button);
        
        // Initialize Discord components
        discordLoginButton = view.findViewById(R.id.discord_login_button);
        discordStatusText = view.findViewById(R.id.discord_status_text);
        discordUserText = view.findViewById(R.id.discord_user_text);
        
        // Initialize Discord manager
        discordManager = new DiscordManager(getActivity()); // Use getActivity() instead of getContext()
        discordManager.setCallback(this);
        discordManager.setFragment(this); // Set fragment reference for startActivityForResult
        
        // Initialize the global RPC helper
        DiscordRPCHelper.getInstance().initialize(discordManager);
        
        // Initialize Discord RPC with the helper
        if (discordManager != null) {
            DiscordRPCHelper.getInstance().initializeRPC(discordManager.getDiscordRPC());
        }
        
        // Initialize handler
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Load saved package name
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", 0);
        String savedPackageName = prefs.getString(PREF_PACKAGE_NAME, DEFAULT_PACKAGE_NAME);
        packageNameEdit.setText(savedPackageName);
        
        // Save package name when text changes
        packageNameEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savePackageName();
            }
        });
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Setup Discord login button
        setupDiscordButton();
        
        // Update Discord UI immediately
        updateDiscordUI();
        
        // If already logged in, start RPC
        if (discordManager.isLoggedIn()) {
            Log.d(TAG, "User already logged in, starting RPC...");
            discordManager.startRPC();
        }
        
        return view;
    }
    
    private void setupButtonListeners() {
        // Add themes button listener - simple fragment replacement
        themesButton.setOnClickListener(v -> {
            try {
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_fade_in_right,  
                        R.anim.slide_out_right, 
                        R.anim.slide_in_left,   
                        R.anim.slide_out_left 
                    )
                    .replace(android.R.id.content, new ThemesFragment())
                    .addToBackStack(null)
                    .commit();
                
                Log.d(TAG, "Opening themes fragment");
            } catch (Exception e) {
                Log.e(TAG, "Error opening themes", e);
                Toast.makeText(getContext(), "Unable to open themes", Toast.LENGTH_SHORT).show();
            }
        });
        
        configurationButton.setOnClickListener(v -> {
            try {
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_fade_in_right,  
                        R.anim.slide_out_right, 
                        R.anim.slide_in_left,   
                        R.anim.slide_out_left 
                    )
                    .replace(android.R.id.content, new ConfigurationFragment())
                    .addToBackStack(null)
                    .commit();
                
                Log.d(TAG, "Opening themes fragment");
            } catch (Exception e) {
                Log.e(TAG, "Error opening themes", e);
                Toast.makeText(getContext(), "Unable to open themes", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Add about button listener - simple fragment replacement
        aboutButton.setOnClickListener(v -> {
            try {
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,  
                        R.anim.slide_out_right, 
                        R.anim.slide_in_left,   
                        R.anim.slide_out_left 
                    )
                    .replace(android.R.id.content, new AboutFragment())
                    .addToBackStack(null)
                    .commit();
                
                Log.d(TAG, "Opening about fragment");
            } catch (Exception e) {
                Log.e(TAG, "Error opening about", e);
                Toast.makeText(getContext(), "Unable to open about", Toast.LENGTH_SHORT).show();
            }
        });
        supportButton.setOnClickListener(v -> {
            try {
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,  
                        R.anim.slide_out_right, 
                        R.anim.slide_in_left,   
                        R.anim.slide_out_left 
                    )
                    .replace(android.R.id.content, new SupportFragment())
                    .addToBackStack(null)
                    .commit();
                
                Log.d(TAG, "Opening support fragment");
            } catch (Exception e) {
                Log.e(TAG, "Error opening support", e);
                Toast.makeText(getContext(), "Unable to open support", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupDiscordButton() {
        discordLoginButton.setOnClickListener(v -> {
            if (discordManager.isLoggedIn()) {
                // Show logout confirmation
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Logout from Discord")
                    .setMessage("Are you sure you want to logout from Discord? This will also disconnect Rich Presence.")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        Log.d(TAG, "User confirmed logout");
                        discordManager.logout();
                        // UI will be updated via callback
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            } else {
                // Disable button during login process
                Log.d(TAG, "Starting Discord login process from button click");
                discordLoginButton.setEnabled(false);
                discordLoginButton.setText("Connecting...");
                discordManager.login();
                
                // Add a timeout to reset the button if something goes wrong
                mainHandler.postDelayed(() -> {
                    if (isAdded() && !discordManager.isLoggedIn() && 
                        discordLoginButton.getText().toString().equals("Connecting...")) {
                        Log.w(TAG, "Discord login timeout - resetting button");
                        updateDiscordUI();
                    }
                }, 30000); // 30 second timeout
            }
        });
    }
    
    private void updateDiscordUI() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot update Discord UI");
            return;
        }
        
        Log.d(TAG, "Updating Discord UI, isLoggedIn: " + discordManager.isLoggedIn());
        
        if (discordManager.isLoggedIn()) {
            DiscordManager.DiscordUser user = discordManager.getCurrentUser();
            if (user != null) {
                discordStatusText.setText("Connected");
                discordStatusText.setTextColor(0xFF4CAF50); // Green
                discordUserText.setText("Logged in as: " + user.displayName);
                discordUserText.setVisibility(View.VISIBLE);
                discordLoginButton.setText("Logout");
                discordLoginButton.setEnabled(true);
                
                // Set red color for logout button
                discordLoginButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336)); // Red for logout
                
                Log.d(TAG, "Discord UI updated for logged in user: " + user.displayName);
            }
        } else {
            discordStatusText.setText("Not connected");
            discordStatusText.setTextColor(0xFFF44336); // Red
            discordUserText.setVisibility(View.GONE);
            discordLoginButton.setText("Login with Discord");
            discordLoginButton.setEnabled(true);
            
            // Set Discord brand color for login
            discordLoginButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF5865F2)); // Discord blue
            
            Log.d(TAG, "Discord UI updated for logged out state");
        }
    }
    
    // Discord callback methods
    @Override
    public void onLoginSuccess(DiscordManager.DiscordUser user) {
        Log.i(TAG, "Discord login successful for user: " + user.displayName);
        
        if (isAdded()) {
            Toast.makeText(getContext(), "Successfully logged in as " + user.displayName, Toast.LENGTH_SHORT).show();
            
            // Update UI on main thread
            mainHandler.post(this::updateDiscordUI);
            
            Log.i(TAG, "Discord login successful, UI updated");
        } else {
            Log.w(TAG, "Fragment not attached during login success");
        }
    }
    
    @Override
    public void onLoginError(String error) {
        Log.e(TAG, "Discord login error: " + error);
        
        if (isAdded()) {
            Toast.makeText(getContext(), "Discord login failed: " + error, Toast.LENGTH_LONG).show();
            
            // Update UI on main thread
            mainHandler.post(this::updateDiscordUI);
        } else {
            Log.w(TAG, "Fragment not attached during login error");
        }
    }
    
    @Override
    public void onLogout() {
        Log.i(TAG, "Discord logout callback received");
        
        if (isAdded()) {
            Toast.makeText(getContext(), "Logged out from Discord", Toast.LENGTH_SHORT).show();
            
            // Update UI on main thread
            mainHandler.post(this::updateDiscordUI);
        }
    }
    
    @Override
    public void onRPCConnected() {
        Log.i(TAG, "Discord RPC connected");
        
        if (isAdded()) {
            Toast.makeText(getContext(), "Discord Rich Presence connected!", Toast.LENGTH_SHORT).show();
            
            // Show a subtle indication that RPC is working
            if (discordUserText.getVisibility() == View.VISIBLE) {
                String currentText = discordUserText.getText().toString();
                if (!currentText.contains("Rich Presence")) {
                    discordUserText.setText(currentText + " • Rich Presence Active");
                }
            }
            
            // Set initial presence when RPC connects
            discordManager.updatePresence("Browsing Settings", "Configuring Xelo Client");
        }
    }
    
    @Override
    public void onRPCDisconnected() {
        Log.i(TAG, "Discord RPC disconnected");
        
        if (isAdded()) {
            // Remove RPC indication from user text
            if (discordUserText.getVisibility() == View.VISIBLE) {
                String currentText = discordUserText.getText().toString();
                if (currentText.contains(" • Rich Presence Active")) {
                    discordUserText.setText(currentText.replace(" • Rich Presence Active", ""));
                }
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "onActivityResult called: requestCode=" + requestCode + 
              ", resultCode=" + resultCode + ", data=" + (data != null ? "present" : "null"));
        
        // Handle Discord login result
        if (requestCode == DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE) {
            Log.d(TAG, "Processing Discord login activity result");
            
            if (discordManager != null) {
                try {
                    discordManager.handleLoginResult(requestCode, resultCode, data);
                    Log.d(TAG, "Discord login result handled by manager");
                } catch (Exception e) {
                    Log.e(TAG, "Error handling Discord login result", e);
                    Toast.makeText(getContext(), "Error handling login result: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateDiscordUI(); // Reset UI on error
                }
            } else {
                Log.e(TAG, "Discord manager is null in onActivityResult");
                updateDiscordUI(); // Reset UI
            }
        } else {
            Log.d(TAG, "Not a Discord login result, ignoring (requestCode: " + requestCode + ")");
        }
    }
    
    private void savePackageName() {
        String packageName = packageNameEdit.getText().toString().trim();
        if (!packageName.isEmpty()) {
            SharedPreferences prefs = requireContext().getSharedPreferences("settings", 0);
            prefs.edit().putString(PREF_PACKAGE_NAME, packageName).apply();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Update Discord RPC when fragment resumes
        DiscordRPCHelper.getInstance().updateMenuPresence("Settings");
        
        // Force UI update to ensure consistency
        updateDiscordUI();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        savePackageName();
        
        // Update Discord RPC when leaving settings
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Don't destroy the discord manager here as it might be used elsewhere
        // The cleanup should happen at app level
    }
    
    // Helper method to update Discord presence from other activities
    public void updateDiscordPresence(String activity, String details) {
        if (discordManager != null && discordManager.isRPCConnected()) {
            discordManager.updatePresence(activity, details);
        }
    }
    
    // Method to get the Discord manager instance for use in other fragments/activities
    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}