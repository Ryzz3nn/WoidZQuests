package com.ryzz3nn.woidzquests.managers;

import com.ryzz3nn.woidzquests.WoidZQuests;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks player online time (playtime) for SURVIVAL/TIMED quests
 * Inspired by BetonQuest's flexible tracking system
 */
public class PlaytimeTracker {
    
    private final WoidZQuests plugin;
    private final Map<UUID, Long> sessionStartTimes;
    private final Map<UUID, Long> totalPlaytime;
    private BukkitTask trackerTask;
    
    // Track every 20 seconds (1 minute = 3 ticks of this)
    private static final long TRACKING_INTERVAL = 20L * 20L; // 20 seconds in ticks
    
    public PlaytimeTracker(WoidZQuests plugin) {
        this.plugin = plugin;
        this.sessionStartTimes = new HashMap<>();
        this.totalPlaytime = new HashMap<>();
    }
    
    /**
     * Start tracking playtime for all online players
     */
    public void startTracking() {
        if (trackerTask != null) {
            trackerTask.cancel();
        }
        
        // Initialize tracking for all currently online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            startTracking(player.getUniqueId());
        }
        
        // Schedule repeating task to update playtime
        trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlaytime(player.getUniqueId());
            }
        }, TRACKING_INTERVAL, TRACKING_INTERVAL);
        
        plugin.getLogger().info("Playtime tracking started (checking every " + (TRACKING_INTERVAL / 20) + " seconds)");
    }
    
    /**
     * Stop tracking playtime
     */
    public void stopTracking() {
        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }
        
        // Save final playtime for all tracked players
        for (UUID uuid : sessionStartTimes.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updatePlaytime(uuid);
            }
        }
        
        sessionStartTimes.clear();
        totalPlaytime.clear();
        
        plugin.getLogger().info("Playtime tracking stopped");
    }
    
    /**
     * Start tracking playtime for a specific player (when they join)
     */
    public void startTracking(UUID playerUuid) {
        sessionStartTimes.put(playerUuid, System.currentTimeMillis());
        totalPlaytime.putIfAbsent(playerUuid, 0L);
    }
    
    /**
     * Stop tracking playtime for a specific player (when they leave)
     */
    public void stopTracking(UUID playerUuid) {
        updatePlaytime(playerUuid);
        sessionStartTimes.remove(playerUuid);
    }
    
    /**
     * Update playtime and add progress to SURVIVAL/TIMED quests
     */
    private void updatePlaytime(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        
        Long sessionStart = sessionStartTimes.get(playerUuid);
        if (sessionStart == null) {
            return;
        }
        
        // Calculate time elapsed since last check (in milliseconds)
        long currentTime = System.currentTimeMillis();
        long elapsedMs = currentTime - sessionStart;
        long elapsedMinutes = elapsedMs / (1000 * 60); // Convert to minutes
        
        // Only update if at least 1 minute has passed
        if (elapsedMinutes >= 1) {
            // Update session start time
            sessionStartTimes.put(playerUuid, currentTime);
            
            // Update total playtime
            long currentTotal = totalPlaytime.getOrDefault(playerUuid, 0L);
            totalPlaytime.put(playerUuid, currentTotal + elapsedMinutes);
            
            // Add progress to SURVIVAL quests for time alive
            try {
                plugin.getDailyQuestManager().addProgress(
                    playerUuid, 
                    com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.SURVIVAL, 
                    "TIME_ALIVE", 
                    (int) elapsedMinutes
                );
                
                plugin.getWeeklyQuestManager().addProgress(
                    playerUuid, 
                    "SURVIVAL", 
                    "TIME_ALIVE", 
                    (int) elapsedMinutes
                );
                
                plugin.getGlobalQuestManager().addProgress(
                    playerUuid, 
                    "SURVIVAL", 
                    "TIME_ALIVE", 
                    elapsedMinutes
                );
                
                // Also track in old quest system
                Map<String, Object> progressData = new HashMap<>();
                progressData.put("minutes", elapsedMinutes);
                progressData.put("world", player.getWorld().getName());
                
                plugin.getQuestManager().addProgress(
                    playerUuid, 
                    com.ryzz3nn.woidzquests.models.Quest.QuestType.SURVIVAL, 
                    progressData, 
                    (int) elapsedMinutes
                );
                
                plugin.getQuestManager().addProgress(
                    playerUuid, 
                    com.ryzz3nn.woidzquests.models.Quest.QuestType.TIMED, 
                    progressData, 
                    (int) elapsedMinutes
                );
                
                // Update player statistics
                plugin.getPlayerDataManager().getPlayerData(playerUuid)
                    .addStatistic("time_played_minutes", (int) elapsedMinutes);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to update playtime progress for " + playerUuid + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Get total playtime for a player in this session
     */
    public long getPlaytime(UUID playerUuid) {
        return totalPlaytime.getOrDefault(playerUuid, 0L);
    }
    
    /**
     * Reset playtime tracking for a player
     */
    public void resetPlaytime(UUID playerUuid) {
        totalPlaytime.put(playerUuid, 0L);
        sessionStartTimes.put(playerUuid, System.currentTimeMillis());
    }
}

