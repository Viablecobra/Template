package com.origin.launcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Typeface;
import android.view.Gravity;
import android.content.Intent;

import com.origin.launcher.InbuiltModsActivity;

public class ModulesFragment extends BaseThemedFragment {
    
    private File configFile;
    private ScrollView modulesScrollView;
    private LinearLayout modulesContainer;
    private List<ModuleItem> moduleItems;
    
    // Module data class
    private static class ModuleItem {
        private String name;
        private String description;
        private String configKey;
        private boolean enabled;
        
        public ModuleItem(String name, String description, String configKey) {
            this.name = name;
            this.description = description;
            this.configKey = configKey;
            this.enabled = false; // Default to disabled
        }
        
        // Getters and setters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getConfigKey() { return configKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modules, container, false);
        
        // Initialize back button
        initializeBackButton(view);
        
        // Initialize modules
        initializeModules(view);
        
        return view;
    }
    
    private void initializeBackButton(View view) {
        ImageView backButton = view.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                try {
                    requireActivity().getSupportFragmentManager().popBackStack();
                } catch (Exception e) {
                    // Handle error gracefully
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            });
        }
    }
    
    private void initializeModules(View view) {
        // Initialize config file path
        configFile = new File("/storage/emulated/0/games/xelo_client/xelo_mods/config.json");
        
        // Get ScrollView and container
        modulesScrollView = view.findViewById(R.id.modulesScrollView);
        modulesContainer = view.findViewById(R.id.modulesContainer);
        
        // Apply theme background to ScrollView and container
        refreshScrollViewBackground();
        
        if (modulesContainer != null) {
            // Initialize module items
            moduleItems = new ArrayList<>();
        
        moduleItems.add(new ModuleItem(
                    "In-built Mods",
                    "Manage Xelo in-built mods (AutoSprint, Quick Drop, etc.)",
                    "inbuilt_mods_entry"
            ));
        
            moduleItems.add(new ModuleItem("No hurt cam", "allows you to toggle the in-game hurt cam", "Nohurtcam"));
            moduleItems.add(new ModuleItem("No Fog", "(Doesnt work with fullbright) allows you to toggle the in-game fog", "Nofog"));
            moduleItems.add(new ModuleItem("Better Brightness", "allows you to see in the dark", "better_brightness"));
            moduleItems.add(new ModuleItem("Particles Disabler", "allows you to toggle the in-game particles", "particles_disabler"));
            moduleItems.add(new ModuleItem("Java Fancy Clouds", "Changes the clouds to Java Fancy Clouds", "java_clouds"));
            moduleItems.add(new ModuleItem("Java Cubemap", "improves the in-game cubemap bringing it abit lower", "java_cubemap"));
            moduleItems.add(new ModuleItem("Classic Vanilla skins", "Disables the newly added skins by mojang", "classic_skins"));
            moduleItems.add(new ModuleItem("No flipbook animation", "optimizes your fps by disabling block animation", "no_flipbook_animations"));
            moduleItems.add(new ModuleItem("No Shadows", "optimizes your fps by disabling shadows", "no_shadows"));
            moduleItems.add(new ModuleItem("Xelo Title", "Changes the Start screen title image", "xelo_title"));
            moduleItems.add(new ModuleItem("2x tpp view", "doubles your third person view radius, letting you see more than you're supposed to", "double_tppview"));
            moduleItems.add(new ModuleItem("White Block Outline", "changes the block selection outline to white", "white_block_outline"));
            moduleItems.add(new ModuleItem("No pumpkin overlay", "disables the dark blurry overlay when wearing pumpkin", "no_pumpkin_overlay"));
            moduleItems.add(new ModuleItem("No spyglass overlay", "disables the spyglass overlay when using spyglass", "no_spyglass_overlay"));
moduleItems.add(new ModuleItem("Custom CrossHair", "lets you use your own CrossHair", "custom_cross_hair"));
            
            // Load current config state and populate modules
            loadModuleStates();
            populateModules();
        }
    }
    
    private void populateModules() {
        if (modulesContainer == null) return;
        
        // Clear existing modules
        modulesContainer.removeAllViews();
        
        // Add each module as a card view with spacing
        for (int i = 0; i < moduleItems.size(); i++) {
            ModuleItem module = moduleItems.get(i);
            View moduleView = createModuleView(module);
            modulesContainer.addView(moduleView);
            
            // Add spacing between cards
            if (i < moduleItems.size() - 1) {
                View spacer = new View(getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    (int) (12 * getResources().getDisplayMetrics().density)
                );
                spacer.setLayoutParams(params);
                modulesContainer.addView(spacer);
            }
        }
    }
    
private View createModuleView(ModuleItem module) {
    // Create card layout (EXACTLY matching ThemesFragment pattern)
    MaterialCardView moduleCard = new MaterialCardView(requireContext());
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    moduleCard.setLayoutParams(cardParams);
    moduleCard.setRadius(12 * getResources().getDisplayMetrics().density);
    moduleCard.setCardElevation(0); // Remove elevation for flat design
    moduleCard.setClickable(true);
    moduleCard.setFocusable(true);

    // Apply theme colors to card (exactly like ThemesFragment)
    ThemeUtils.applyThemeToCard(moduleCard, requireContext());
    moduleCard.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));

    // Main container (EXACTLY matching ThemesFragment)
    LinearLayout mainLayout = new LinearLayout(requireContext());
    mainLayout.setOrientation(LinearLayout.HORIZONTAL);
    mainLayout.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density)
    );
    mainLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

    // Text container (EXACTLY matching ThemesFragment - text comes first)
    LinearLayout textLayout = new LinearLayout(requireContext());
    textLayout.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
    );
    textLayout.setLayoutParams(textParams);

    // Module name (EXACTLY matching ThemesFragment pattern)
    TextView moduleNameText = new TextView(requireContext());
    moduleNameText.setText(module.getName());
    moduleNameText.setTextSize(16);
    moduleNameText.setTypeface(null, android.graphics.Typeface.BOLD);
    ThemeUtils.applyThemeToTextView(moduleNameText, "onSurface");

    // Module description (EXACTLY matching ThemesFragment pattern)
    TextView moduleDescriptionText = new TextView(requireContext());
    moduleDescriptionText.setText(module.getDescription());
    moduleDescriptionText.setTextSize(14);
    ThemeUtils.applyThemeToTextView(moduleDescriptionText, "onSurfaceVariant");
    LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    descParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
    moduleDescriptionText.setLayoutParams(descParams);

    textLayout.addView(moduleNameText);
    textLayout.addView(moduleDescriptionText);

    // Right side container for switch / open text
    LinearLayout rightContainer = new LinearLayout(requireContext());
    rightContainer.setOrientation(LinearLayout.HORIZONTAL);
    rightContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    rightParams.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));
    rightContainer.setLayoutParams(rightParams);

    if ("inbuilt_mods_entry".equals(module.getConfigKey())) {
        // This card opens the Inbuilt Mods screen
        TextView openText = new TextView(requireContext());
        openText.setText("Open");
        openText.setTextSize(14);
        openText.setTypeface(null, android.graphics.Typeface.BOLD);
        ThemeUtils.applyThemeToTextView(openText, "primary");
        rightContainer.addView(openText);

        moduleCard.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(
                        requireContext(),
                        com.origin.launcher.InbuiltModsActivity.class
                ));
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Failed to open in-built mods: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    } else {
        // Module switch - PRESERVING CONFIG FUNCTIONALITY
        MaterialSwitch moduleSwitch = new MaterialSwitch(requireContext());
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        moduleSwitch.setLayoutParams(switchParams);
        moduleSwitch.setChecked(module.isEnabled());
        moduleSwitch.setClickable(true);
        moduleSwitch.setFocusable(true);

        // Apply theme to the switch - PRESERVING CONFIG FUNCTIONALITY
        ThemeUtils.applyThemeToSwitch(moduleSwitch, requireContext());

        // PRESERVE the config editing functionality
        moduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            module.setEnabled(isChecked);
            onModuleToggle(module, isChecked);
        });

        rightContainer.addView(moduleSwitch);
    }

    // Add both sides to main layout and to card
    mainLayout.addView(textLayout);
    mainLayout.addView(rightContainer);
    moduleCard.addView(mainLayout);

    return moduleCard;
}
    
    private void onModuleToggle(ModuleItem module, boolean isEnabled) {
        updateConfigFile(module.getConfigKey(), isEnabled);
        Toast.makeText(requireContext(), 
            module.getName() + " " + (isEnabled ? "enabled" : "disabled"), 
            Toast.LENGTH_SHORT).show();
    }
    
    private void loadModuleStates() {
        try {
            if (!configFile.exists()) {
                // Create directory if it doesn't exist
                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                // Create default config
                createDefaultConfig();
                return;
            }
            
            // Read existing config
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            
            JSONObject config = new JSONObject(content.toString());
            
            // Update module states
            for (ModuleItem module : moduleItems) {
                if (config.has(module.getConfigKey())) {
                    module.setEnabled(config.getBoolean(module.getConfigKey()));
                }
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to load module config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void createDefaultConfig() {
        try {
            // Create parent directory if it doesn't exist
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    Toast.makeText(requireContext(), "Failed to create config directory", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            JSONObject defaultConfig = new JSONObject();
            defaultConfig.put("Nohurtcam", false);
            defaultConfig.put("Nofog", false);
            defaultConfig.put("better_brightness", false);
            defaultConfig.put("particles_disabler", false);
            defaultConfig.put("java_clouds", false);
            defaultConfig.put("java_cubemap", false);
            defaultConfig.put("classic_skins", false);
            defaultConfig.put("white_block_outline", false);
            defaultConfig.put("no_flipbook_animations", false);
            defaultConfig.put("no_shadows", false);
            defaultConfig.put("no_spyglass_overlay", false);
            defaultConfig.put("no_pumpkin_overlay", false);
            defaultConfig.put("double_tppview", false);
            defaultConfig.put("xelo_title", true);

defaultConfig.put("custom_cross_hair", false);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultConfig.toString(2)); // Pretty print with indent
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to create default config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void updateConfigFile(String key, boolean value) {
        try {
            JSONObject config;
            
            if (configFile.exists()) {
                // Read existing config
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                }
                config = new JSONObject(content.toString());
            } else {
                // Create new config and ensure directory exists
                config = new JSONObject();
                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    if (!created) {
                        Toast.makeText(requireContext(), "Failed to create config directory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            
            // Update the specific key
            config.put(key, value);
            
            // Write back to file
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(config.toString(2)); // Pretty print with indent
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to update config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onApplyTheme() {
        // Apply theme to the root view background
        View rootView = getView();
        if (rootView != null) {
            rootView.setBackgroundColor(ThemeManager.getInstance().getColor("background"));
        }
        
        // Apply theme to back button
        ImageView backButton = rootView != null ? rootView.findViewById(R.id.back_button) : null;
        if (backButton != null) {
            backButton.setColorFilter(ThemeManager.getInstance().getColor("onBackground"));
        }
        
        // Apply theme to ScrollView and modules container background
        refreshScrollViewBackground();
        
        // Refresh all module cards
        if (modulesContainer != null) {
            populateModules();
        }
    }
    
    /**
     * Refresh ScrollView and container background colors
     */
    private void refreshScrollViewBackground() {
        try {
            if (modulesScrollView != null) {
                // Make ScrollView background completely transparent
                modulesScrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                modulesScrollView.setBackground(null); // Remove any drawable background
            }
            if (modulesContainer != null) {
                // Make container background completely transparent
                modulesContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                modulesContainer.setBackground(null); // Remove any drawable background
            }
        } catch (Exception e) {
            // Fallback to transparent background
            if (modulesScrollView != null) {
                modulesScrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                try {
                    modulesScrollView.setBackground(null);
                } catch (Exception ignored) {}
            }
            if (modulesContainer != null) {
                modulesContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                try {
                    modulesContainer.setBackground(null);
                } catch (Exception ignored) {}
            }
        }
    }
}