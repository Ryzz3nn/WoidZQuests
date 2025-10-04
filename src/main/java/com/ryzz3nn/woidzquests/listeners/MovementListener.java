package com.ryzz3nn.woidzquests.listeners;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.Quest;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MovementListener implements Listener {
    
    private final WoidZQuests plugin;
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastBiomes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMovementTime = new ConcurrentHashMap<>();
    
    public MovementListener(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        
        if (to == null || !hasMovedToNewBlock(from, to)) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Throttle movement tracking
        long throttleTime = plugin.getConfigManager().getInt("quests.anti-cheese.movement-throttle", 20) * 50L; // Convert ticks to ms
        if (lastMovementTime.containsKey(playerUuid) && 
            currentTime - lastMovementTime.get(playerUuid) < throttleTime) {
            return;
        }
        
        lastMovementTime.put(playerUuid, currentTime);
        
        // Calculate distance traveled
        Location lastLocation = lastLocations.get(playerUuid);
        if (lastLocation != null && lastLocation.getWorld().equals(to.getWorld())) {
            double distance = lastLocation.distance(to);
            
            // Create progress data for distance quests
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("distance", distance);
            progressData.put("world", to.getWorld().getName());
            progressData.put("biome", to.getBlock().getBiome().getKey().getKey());
            
            // Add progress to exploration quests
            plugin.getQuestManager().addProgress(playerUuid, Quest.QuestType.EXPLORATION, progressData, (int) distance);
            
            // Update player statistics
            plugin.getPlayerDataManager().getPlayerData(playerUuid)
                .addStatistic("distance_traveled", (int) distance);
        }
        
        lastLocations.put(playerUuid, to.clone());
        
        // Check for biome changes
        String currentBiome = to.getBlock().getBiome().getKey().getKey();
        String lastBiome = lastBiomes.get(playerUuid);
        
        if (lastBiome == null || !lastBiome.equals(currentBiome)) {
            lastBiomes.put(playerUuid, currentBiome);
            
            // Create progress data for biome discovery
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("biome", currentBiome);
            progressData.put("world", to.getWorld().getName());
            
            // Add progress to biome discovery quests
            plugin.getQuestManager().addProgress(playerUuid, Quest.QuestType.EXPLORATION, progressData, 1);
            
            // Update player statistics
            plugin.getPlayerDataManager().getPlayerData(playerUuid)
                .addStatistic("biomes_discovered", 1);
            plugin.getPlayerDataManager().getPlayerData(playerUuid)
                .addStatistic("biome_" + currentBiome.toLowerCase(), 1);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Update last location for new world
        lastLocations.put(playerUuid, player.getLocation().clone());
        
        // Reset biome tracking for new world
        lastBiomes.put(playerUuid, player.getLocation().getBlock().getBiome().getKey().getKey());
        
        // Create progress data for world change
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("world", player.getWorld().getName());
        progressData.put("dimension", player.getWorld().getEnvironment().name());
        
        // Add progress to exploration quests
        plugin.getQuestManager().addProgress(playerUuid, Quest.QuestType.EXPLORATION, progressData, 1);
        
        // Update player statistics
        plugin.getPlayerDataManager().getPlayerData(playerUuid)
            .addStatistic("worlds_visited", 1);
        plugin.getPlayerDataManager().getPlayerData(playerUuid)
            .addStatistic("world_" + player.getWorld().getName().toLowerCase(), 1);
    }
    
    private boolean hasMovedToNewBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX() || 
               from.getBlockY() != to.getBlockY() || 
               from.getBlockZ() != to.getBlockZ();
    }
}
