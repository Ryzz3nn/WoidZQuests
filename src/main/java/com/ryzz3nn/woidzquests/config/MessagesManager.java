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
public class MessagesManager {
    
    private final WoidZQuests plugin;
    private File messagesFile;
    private FileConfiguration messages;
    
    public MessagesManager(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
            
            // Create plugin folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Load or create messages
            if (!messagesFile.exists()) {
                plugin.saveResource("messages.yml", false);
            }
            
            messages = YamlConfiguration.loadConfiguration(messagesFile);
            
            // Auto-update messages with new values
            updateMessages();
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize messages", e);
            return false;
        }
    }
    
    public void reload() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        updateMessages();
    }
    
    private void updateMessages() {
        try {
            // Load default messages from resources
            InputStream defaultMessagesStream = plugin.getResource("messages.yml");
            if (defaultMessagesStream == null) {
                return;
            }
            
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultMessagesStream, StandardCharsets.UTF_8)
            );
            
            boolean updated = false;
            
            // Add missing keys from default messages
            for (String key : defaultMessages.getKeys(true)) {
                if (!messages.contains(key)) {
                    messages.set(key, defaultMessages.get(key));
                    updated = true;
                    plugin.getLogger().info("Added missing message key: " + key);
                }
            }
            
            // Save if updated
            if (updated) {
                save();
                plugin.getLogger().info("Messages updated with new entries!");
            }
            
            defaultMessagesStream.close();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to update messages", e);
        }
    }
    
    public void save() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save messages", e);
        }
    }
    
    public String getMessage(String path) {
        return messages.getString(path, "&7[&c&l!&7] Message not found: " + path);
    }
    
    public List<String> getMessageList(String path) {
        return messages.getStringList(path);
    }
    
    // Common messages
    public String getPrefix() {
        return getMessage("prefix");
    }
    
    public String getNoPermission() {
        return getMessage("no-permission");
    }
    
    public String getReloadSuccess() {
        return getMessage("reload-success");
    }
    
    public String getQuestCompleted() {
        return getMessage("quest.completed");
    }
    
    public String getQuestProgress() {
        return getMessage("quest.progress");
    }
    
    public String getQuestStarted() {
        return getMessage("quest.started");
    }
    
    public String getQuestFailed() {
        return getMessage("quest.failed");
    }
    
    public String getQuestRewardClaimed() {
        return getMessage("quest.reward-claimed");
    }
    
    public String getQuestNoActive() {
        return getMessage("quest.no-active");
    }
    
    public String getQuestMaxActive() {
        return getMessage("quest.max-active");
    }
    
    public String getQuestRerollSuccess() {
        return getMessage("quest.reroll-success");
    }
    
    public String getQuestRerollFailed() {
        return getMessage("quest.reroll-failed");
    }
    
    public String getQuestRerollNoLeft() {
        return getMessage("quest.reroll-no-left");
    }
}
