package com.ryzz3nn.woidzquests.integrations;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.Quest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class WoidZFishingIntegration implements Listener {
    
    private final WoidZQuests plugin;
    private boolean enabled = false;
    
    public WoidZFishingIntegration(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        Plugin woidzFishing = Bukkit.getPluginManager().getPlugin("WoidZFishing");
        if (woidzFishing != null && woidzFishing.isEnabled()) {
            try {
                // Test if we can access the FishCatchEvent class
                Class<?> fishCatchEventClass = Class.forName("dev.ryzz3nn.woidzfishing.events.FishCatchEvent");
                
                // Register a dynamic event listener using reflection
                registerDynamicEventListener(fishCatchEventClass);
                enabled = true;
                
                plugin.getLogger().info("WoidZFishing integration enabled!");
                return true;
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("WoidZFishing found but FishCatchEvent class not available. Using fallback fishing detection.");
                return false;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to initialize WoidZFishing integration: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private void registerDynamicEventListener(Class<?> eventClass) throws Exception {
        // Create a dynamic listener using Bukkit's event system
        Bukkit.getPluginManager().registerEvent(
            (Class<? extends org.bukkit.event.Event>) eventClass,
            this,
            org.bukkit.event.EventPriority.MONITOR,
            (listener, event) -> {
                if (eventClass.isInstance(event)) {
                    handleFishCatchEvent(event);
                }
            },
            plugin,
            false
        );
    }
    
    private void handleFishCatchEvent(Object event) {
        if (!enabled) return;
        
        // Use reflection to handle the FishCatchEvent since we can't directly import it
        try {
            Class<?> fishCatchEventClass = Class.forName("dev.ryzz3nn.woidzfishing.events.FishCatchEvent");
            
            if (!fishCatchEventClass.isInstance(event)) {
                return;
            }
            
            // Get player using reflection
            Player player = (Player) fishCatchEventClass.getMethod("getPlayer").invoke(event);
            
            // Get fish information using reflection
            String fishName = (String) fishCatchEventClass.getMethod("getFishName").invoke(event);
            String rarity = (String) fishCatchEventClass.getMethod("getRarity").invoke(event);
            int tier = (Integer) fishCatchEventClass.getMethod("getTier").invoke(event);
            double length = (Double) fishCatchEventClass.getMethod("getLength").invoke(event);
            double weight = (Double) fishCatchEventClass.getMethod("getWeight").invoke(event);
            double value = (Double) fishCatchEventClass.getMethod("getValue").invoke(event);
            boolean isUltraRare = (Boolean) fishCatchEventClass.getMethod("isUltraRare").invoke(event);
            
            // Get catch location using reflection
            Object catchLocation = fishCatchEventClass.getMethod("getCatchLocation").invoke(event);
            String biome = ((org.bukkit.Location) catchLocation).getBlock().getBiome().getKey().getKey();
            String world = ((org.bukkit.Location) catchLocation).getWorld().getName();
            
            // Create progress data for quest tracking
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("fish_name", fishName);
            progressData.put("rarity", rarity);
            progressData.put("tier", tier);
            progressData.put("length", length);
            progressData.put("weight", weight);
            progressData.put("value", value);
            progressData.put("ultra_rare", isUltraRare);
            progressData.put("biome", biome);
            progressData.put("world", world);
            
            // Add progress to fishing quests (old system)
            plugin.getQuestManager().addProgress(player.getUniqueId(), Quest.QuestType.FISHING, progressData, 1);
            
            // Add progress to new quest systems
            addFishingProgress(player, fishName, rarity, tier, isUltraRare, value, length);
            
            // Update player statistics
            plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
                .addStatistic("fish_caught", 1);
            plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
                .addStatistic("fish_caught_" + fishName.toLowerCase().replace(" ", "_"), 1);
            plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
                .addStatistic("fish_caught_" + rarity.toLowerCase(), 1);
            
        } catch (Exception e) {
            // Log error but don't spam console
            plugin.getLogger().fine("Failed to handle WoidZFishing event: " + e.getMessage());
        }
    }
    
    private void addFishingProgress(Player player, String fishName, String rarity, int tier, 
                                  boolean isUltraRare, double value, double length) {
        
        // Generic fish catching progress
        plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
            com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, "ANY_FISH", 1);
        plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", "ANY_FISH", 1);
        plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "ANY_FISH", 1L);
        
        // Specific fish type progress
        String fishTarget = fishName.toUpperCase().replace(" ", "_");
        plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
            com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, fishTarget, 1);
        plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", fishTarget, 1);
        plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", fishTarget, 1L);
        
        // Rarity-based progress
        if (isRareOrAbove(rarity)) {
            plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
                com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, "RARE_FISH", 1);
            plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", "RARE_FISH", 1);
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "RARE_FISH", 1L);
        }
        
        // Ultra rare fish progress
        if (isUltraRare) {
            plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
                com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, "ULTRA_RARE_FISH", 1);
            plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", "ULTRA_RARE_FISH", 1);
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "ULTRA_RARE_FISH", 1L);
        }
        
        // Valuable fish progress (over $100)
        if (value >= 100.0) {
            plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
                com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, "VALUABLE_FISH", 1);
            plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", "VALUABLE_FISH", 1);
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "VALUABLE_FISH", 1L);
        }
        
        // Large fish progress (over 50cm)
        if (length >= 50.0) {
            plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
                com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, "LARGE_FISH", 1);
            plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", "LARGE_FISH", 1);
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "LARGE_FISH", 1L);
        }
        
        // Tier-based progress
        plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
            com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, "TIER_" + tier + "_FISH", 1);
        plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", "TIER_" + tier + "_FISH", 1);
        plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "TIER_" + tier + "_FISH", 1L);
        
        // Tier 3+ progress for daily quests
        if (tier >= 3) {
            plugin.getDailyQuestManager().addProgress(player.getUniqueId(), 
                com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.FISHING, "TIER_3_FISH", 1);
        }
        
        // Tier 5+ progress for weekly quests
        if (tier >= 5) {
            plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "FISHING", "TIER_5_FISH", 1);
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "TIER_5_FISH", 1L);
        }
        
        // Also handle the COD target for existing global quests
        if (fishName.equalsIgnoreCase("Cod") || fishName.contains("Cod")) {
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "FISHING", "COD", 1L);
        }
    }
    
    private boolean isRareOrAbove(String rarity) {
        return rarity.equals("RARE") || rarity.equals("EPIC") || 
               rarity.equals("LEGENDARY") || rarity.equals("ULTRA_RARE");
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
