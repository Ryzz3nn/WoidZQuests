package com.ryzz3nn.woidzquests.config;

import com.ryzz3nn.woidzquests.WoidZQuests;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

@Getter
public class ConfigManager {
    
    private final WoidZQuests plugin;
    private File configFile;
    private FileConfiguration config;
    
    public ConfigManager(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            configFile = new File(plugin.getDataFolder(), "config.yml");
            
            // Create plugin folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Load or create config
            if (!configFile.exists()) {
                plugin.saveResource("config.yml", false);
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Auto-update config with new values
            updateConfig();
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize config", e);
            return false;
        }
    }
    
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        updateConfig();
    }
    
    private void updateConfig() {
        try {
            // Load default config from resources
            InputStream defaultConfigStream = plugin.getResource("config.yml");
            if (defaultConfigStream == null) {
                return;
            }
            
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8)
            );
            
            boolean updated = false;
            
            // Add missing keys from default config
            for (String key : defaultConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                    updated = true;
                    plugin.getLogger().info("Added missing config key: " + key);
                }
            }
            
            // Save if updated
            if (updated) {
                save();
                plugin.getLogger().info("Config updated with new entries!");
            }
            
            defaultConfigStream.close();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to update config", e);
        }
    }
    
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config", e);
        }
    }
    
    // Config getters with defaults
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    // Specific config values
    public String getServerName() {
        return getString("server.name", "WoidZ Dev Server");
    }
    
    public String getDatabasePath() {
        return getString("database.path", "plugins/WoidZQuests/data.db");
    }
    
    public int getMaxActiveQuests() {
        return getInt("quests.max-active", 3);
    }
    
    public int getDailyQuestRerolls() {
        return getInt("quests.daily-rerolls", 3);
    }
    
    public boolean isDebugEnabled() {
        return getBoolean("debug", false);
    }
    
    public int getQuestGuiSize() {
        return getInt("gui.quest-size", 54);
    }
    
    public String getQuestGuiTitle() {
        return getString("gui.quest-title", "&7[&d&lQuests&7] &7Your Active Quests");
    }
}
