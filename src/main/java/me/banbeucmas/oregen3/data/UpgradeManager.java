package me.banbeucmas.oregen3.data;

import me.banbeucmas.oregen3.Oregen3;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpgradeManager {
    private final Oregen3 plugin;
    private File file;
    private FileConfiguration config;
    private final Map<UUID, Map<String, Integer>> upgrades = new HashMap<>();

    public UpgradeManager(Oregen3 plugin) {
        this.plugin = plugin;
        loadFile();
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadUpgrades();
    }

    private void loadUpgrades() {
        upgrades.clear();
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Integer> islandUpgrades = new HashMap<>();
                for (String upgradeId : config.getConfigurationSection(uuidStr).getKeys(false)) {
                    islandUpgrades.put(upgradeId, config.getInt(uuidStr + "." + upgradeId));
                }
                upgrades.put(uuid, islandUpgrades);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveUpgrades() {
        for (Map.Entry<UUID, Map<String, Integer>> entry : upgrades.entrySet()) {
            for (Map.Entry<String, Integer> upgrade : entry.getValue().entrySet()) {
                config.set(entry.getKey().toString() + "." + upgrade.getKey(), upgrade.getValue());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getUpgradeLevel(UUID islandOwner, String upgradeId) {
        if (!upgrades.containsKey(islandOwner)) return 0;
        return upgrades.get(islandOwner).getOrDefault(upgradeId, 0);
    }

    public void setUpgradeLevel(UUID islandOwner, String upgradeId, int level) {
        upgrades.computeIfAbsent(islandOwner, k -> new HashMap<>()).put(upgradeId, level);
        config.set(islandOwner.toString() + "." + upgradeId, level);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
