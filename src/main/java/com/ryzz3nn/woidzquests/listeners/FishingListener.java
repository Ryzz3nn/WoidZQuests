package com.ryzz3nn.woidzquests.listeners;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.HashMap;
import java.util.Map;

public class FishingListener implements Listener {
    
    private final WoidZQuests plugin;
    
    public FishingListener(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        // Skip if WoidZFishing is handling fishing events
        if (plugin.getWoidZFishingIntegration() != null && plugin.getWoidZFishingIntegration().isEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Create progress data
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("world", player.getWorld().getName());
        progressData.put("biome", player.getLocation().getBlock().getBiome().getKey().getKey());
        
        // Determine fishing environment
        if (event.getHook().getLocation().getBlock().getType().name().contains("LAVA")) {
            progressData.put("environment", "LAVA");
        } else if (player.getWorld().getEnvironment().name().equals("THE_END")) {
            progressData.put("environment", "END");
        } else {
            progressData.put("environment", "WATER");
        }
        
        // Add caught item info if available
        if (event.getCaught() != null) {
            progressData.put("item", event.getCaught().getType().name());
        }
        
        // Add progress to fishing quests (old system)
        plugin.getQuestManager().addProgress(player.getUniqueId(), Quest.QuestType.FISHING, progressData, 1);
        
        // Add progress to new quest systems (vanilla fishing)
        plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
            com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, "ANY_FISH", 1);
        plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", "ANY_FISH", 1);
        plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "ANY_FISH", 1L);
        
        // Handle COD for global quests (vanilla fishing typically catches cod)
        if (event.getCaught() != null && event.getCaught().getType().name().contains("COD")) {
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "COD", 1L);
        }
        
        // Update player statistics
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("fish_caught", 1);
    }
}
