package com.ryzz3nn.woidzquests.listeners;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDateTime;

public class PlayerListener implements Listener {
    
    private final WoidZQuests plugin;
    
    public PlayerListener(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load or create player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        playerData.setUsername(player.getName());
        playerData.setLastSeen(LocalDateTime.now());
        
        // Check for daily/weekly resets
        if (playerData.needsDailyReset()) {
            playerData.resetDailyRerolls(plugin.getConfigManager().getDailyQuestRerolls());
        }
        
        if (playerData.needsWeeklyReset()) {
            playerData.resetWeeklyRerolls(plugin.getConfigManager().getInt("quests.weekly-rerolls", 1));
        }
        
        // Save player data
        plugin.getPlayerDataManager().savePlayerData(playerData);
        
        // Start tracking playtime for TIMED/SURVIVAL quests
        plugin.getPlaytimeTracker().startTracking(player.getUniqueId());
        
        // Auto-assign quests if enabled
        if (plugin.getConfigManager().getBoolean("quests.auto-assign", true)) {
            // TODO: Auto-assign quests
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Stop tracking playtime
        plugin.getPlaytimeTracker().stopTracking(player.getUniqueId());
        
        // Update last seen time
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        playerData.setLastSeen(LocalDateTime.now());
        
        // Save player data
        plugin.getPlayerDataManager().savePlayerData(playerData);
        
        // Remove from cache after a delay to allow for quick reconnects
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getPlayerDataManager().removePlayerData(player.getUniqueId());
        }, 20 * 60); // 1 minute delay
    }
}
