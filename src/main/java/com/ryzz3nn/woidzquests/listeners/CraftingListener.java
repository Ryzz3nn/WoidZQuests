package com.ryzz3nn.woidzquests.listeners;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CraftingListener implements Listener {
    
    private final WoidZQuests plugin;
    
    public CraftingListener(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        ItemStack result = event.getCurrentItem();
        if (result == null) {
            return;
        }
        
        // Create progress data
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("item", result.getType().name());
        progressData.put("amount", result.getAmount());
        
        // Add progress to crafting quests
        plugin.getQuestManager().addProgress(player.getUniqueId(), Quest.QuestType.CRAFTING, progressData, result.getAmount());
        
        // Update player statistics
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("items_crafted", result.getAmount());
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("items_crafted_" + result.getType().name().toLowerCase(), result.getAmount());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        
        // Create progress data
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("item", event.getItemType().name());
        progressData.put("amount", event.getItemAmount());
        
        // Add progress to smelting quests
        plugin.getQuestManager().addProgress(player.getUniqueId(), Quest.QuestType.SMELTING, progressData, event.getItemAmount());
        
        // Update player statistics
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("items_smelted", event.getItemAmount());
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("items_smelted_" + event.getItemType().name().toLowerCase(), event.getItemAmount());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        ItemStack result = event.getCurrentItem();
        if (result == null) {
            return;
        }
        
        // Handle different inventory types
        InventoryType inventoryType = event.getInventory().getType();
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("item", result.getType().name());
        progressData.put("amount", result.getAmount());
        
        Quest.QuestType questType = null;
        String statPrefix = "";
        
        switch (inventoryType) {
            case ANVIL -> {
                questType = Quest.QuestType.ANVIL;
                statPrefix = "anvil_";
            }
            case ENCHANTING -> {
                questType = Quest.QuestType.ENCHANTING;
                statPrefix = "enchanted_";
            }
            case BREWING -> {
                questType = Quest.QuestType.BREWING;
                statPrefix = "brewed_";
            }
            case SMITHING -> {
                questType = Quest.QuestType.SMITHING;
                statPrefix = "smithed_";
            }
        }
        
        if (questType != null) {
            // Add progress to specific quest type
            plugin.getQuestManager().addProgress(player.getUniqueId(), questType, progressData, result.getAmount());
            
            // Update player statistics
            plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
                .addStatistic(statPrefix + "items", result.getAmount());
            plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
                .addStatistic(statPrefix + result.getType().name().toLowerCase(), result.getAmount());
        }
    }
}
