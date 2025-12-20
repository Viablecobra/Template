package com.origin.launcher.Launcher.mods;

import android.os.FileObserver;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.origin.launcher.versions.GameVersion;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ModManager {
    private static volatile ModManager instance;
    private File modsDir;
    private File configFile;
    private final Map<String, Boolean> enabledMap = new LinkedHashMap<>();
    private final List<String> modOrder = new ArrayList<>();
    private FileObserver modDirObserver;
    private GameVersion currentVersion;
    private final MutableLiveData<Void> modsChangedLiveData = new MutableLiveData<>();
    private final Gson gson = new Gson();

    private ModManager() {}

    public static ModManager getInstance() {
        ModManager result = instance;
        if (result == null) {
            synchronized (ModManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new ModManager();
                }
            }
        }
        return result;
    }

    public synchronized void setCurrentVersion(GameVersion version) {
        if (Objects.equals(currentVersion, version)) return;
        stopFileObserver();
        currentVersion = version;

        if (version != null && version.modsDir != null) {
            modsDir = version.modsDir;
            modsDir.mkdirs();
            configFile = new File(modsDir, "mods_config.json");
            loadConfig();
            initFileObserver();
        } else {
            modsDir = null;
            configFile = null;
            enabledMap.clear();
            modOrder.clear();
        }
        notifyModsChanged();
    }

    public GameVersion getCurrentVersion() {
        return currentVersion;
    }

    public List<Mod> getMods() {
        if (modsDir == null) return new ArrayList<>();

        List<Mod> mods = new ArrayList<>();
        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".so"));
        boolean changed = false;

        // Add new mods
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (!enabledMap.containsKey(fileName)) {
                    enabledMap.put(fileName, true);
                    modOrder.add(fileName);
                    changed = true;
                }
            }
        }

        // Remove deleted mods
        modOrder.removeIf(fileName -> {
            File file = new File(modsDir, fileName);
            if (!file.exists()) {
                enabledMap.remove(fileName);
                return true;
            }
            return false;
        });

        for (int i = 0; i < modOrder.size(); i++) {
            String fileName = modOrder.get(i);
            mods.add(new Mod(fileName, enabledMap.getOrDefault(fileName, true), i));
        }

        if (changed) saveConfig();
        return mods;
    }

    public synchronized void setModEnabled(String fileName, boolean enabled) {
        if (modsDir == null) return;
        if (!fileName.endsWith(".so")) fileName += ".so";
        if (enabledMap.containsKey(fileName)) {
            enabledMap.put(fileName, enabled);
            saveConfig();
            notifyModsChanged();
        }
    }

    private void loadConfig() {
        enabledMap.clear();
        modOrder.clear();

        if (!configFile.exists()) {
            updateConfigFromDirectory();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> configList = gson.fromJson(reader, type);

            if (configList != null) {
                for (Map<String, Object> item : configList) {
                    String name = (String) item.get("name");
                    Boolean enabled = (Boolean) item.get("enabled");
                    if (name != null && enabled != null) {
                        enabledMap.put(name, enabled);
                        modOrder.add(name);
                    }
                }
            } else {
                updateConfigFromDirectory();
            }
        } catch (Exception e) {
            updateConfigFromDirectory();
        }
    }

    private void updateConfigFromDirectory() {
        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".so"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                enabledMap.put(fileName, true);
                modOrder.add(fileName);
            }
        }
        saveConfig();
    }

    private void saveConfig() {
        if (configFile == null) return;
        try (FileWriter writer = new FileWriter(configFile)) {
            List<Map<String, Object>> configList = new ArrayList<>();
            for (int i = 0; i < modOrder.size(); i++) {
                String fileName = modOrder.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", fileName);
                item.put("enabled", enabledMap.get(fileName));
                item.put("order", i);
                configList.add(item);
            }
            gson.toJson(configList, writer);
        } catch (Exception ignored) {}
    }

    private void initFileObserver() {
        if (modsDir == null) return;
        modDirObserver = new FileObserver(modsDir.getAbsolutePath(),
                FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String path) {
                notifyModsChanged();
            }
        };
        modDirObserver.startWatching();
    }

    private void stopFileObserver() {
        if (modDirObserver != null) {
            modDirObserver.stopWatching();
            modDirObserver = null;
        }
    }

    public synchronized void deleteMod(String fileName) {
        if (modsDir == null) return;
        if (!fileName.endsWith(".so")) fileName += ".so";

        File modFile = new File(modsDir, fileName);
        if (modFile.exists() && modFile.delete()) {
            enabledMap.remove(fileName);
            modOrder.remove(fileName);
            saveConfig();
            notifyModsChanged();
        }
    }

    public synchronized void reorderMods(List<Mod> reorderedMods) {
        if (modsDir == null) return;

        modOrder.clear();
        for (Mod mod : reorderedMods) {
            modOrder.add(mod.getFileName());
        }
        saveConfig();
        notifyModsChanged();
    }

    private void notifyModsChanged() {
        modsChangedLiveData.postValue(null);
    }

    public MutableLiveData<Void> getModsChangedLiveData() {
        return modsChangedLiveData;
    }

    public synchronized void refreshMods() {
        notifyModsChanged();
    }
}