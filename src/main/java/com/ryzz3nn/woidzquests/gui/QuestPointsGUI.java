package com.ryzz3nn.woidzquests.gui;

import com.ryzz3nn.woidzquests.WoidZQuests;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class QuestPointsGUI implements InventoryHolder {
    
    private final WoidZQuests plugin;
    private final Inventory inventory;
    private final Player player;
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    
    public QuestPointsGUI(WoidZQuests plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        // Get title from messages.yml
        String title = plugin.getMessagesManager().getMessage("gui.titles.quest-points");
        this.inventory = Bukkit.createInventory(this, 27, plugin.parseMessage(title));
        
        setupGUI();
    }
    
    private void setupGUI() {
        // Fill with filler items (supports Nexo custom items)
        // Display name is already removed in getFillerItem() for clean hover
        ItemStack filler = plugin.getFillerItem();
        
        // Fill entire GUI first
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }
        
        // Get player stats
        int questsCompleted = getPlayerTotalQuests(player);
        int dailyCompleted = getPlayerDailyCount(player);
        int weeklyCompleted = getPlayerWeeklyCount(player);
        int globalCompleted = getPlayerGlobalCount(player);
        String balance = getQuestPointsBalance(player);
        
        // TOP ROW: Main QP Display (center)
        inventory.setItem(4, createQPBalanceItem(balance, questsCompleted));
        
        // MIDDLE ROW: Quick Stats
        inventory.setItem(10, createQuickStatsItem(dailyCompleted, weeklyCompleted, globalCompleted));
        inventory.setItem(13, createShopButton());
        inventory.setItem(16, createHowToEarnItem());
        
        // BOTTOM ROW: Back button
        inventory.setItem(22, createBackButton());
    }
    
    private ItemStack createQPBalanceItem(String balance, int totalQuests) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.parseMessage("<light_purple><bold>Quest Points"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(plugin.parseMessage("<!italic><gray>Balance: <light_purple><bold>" + balance + " QP"));
            lore.add(plugin.parseMessage("<!italic><gray>Total Quests: <aqua>" + numberFormat.format(totalQuests)));
            lore.add(Component.empty());
            lore.add(plugin.parseMessage("<!italic><dark_gray>Earn QP by completing quests"));
            lore.add(plugin.parseMessage("<!italic><dark_gray>Spend QP in the shop"));
            lore.add(Component.empty());
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createQuickStatsItem(int daily, int weekly, int global) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.parseMessage("<aqua><bold>Quest Statistics"));
            
            int totalEarned = (daily * 1) + (weekly * 5) + (global * 10);
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(plugin.parseMessage("<!italic><yellow>Daily: <gray>" + numberFormat.format(daily) + " <dark_gray>(1 QP each)"));
            lore.add(plugin.parseMessage("<!italic><gold>Weekly: <gray>" + numberFormat.format(weekly) + " <dark_gray>(5 QP each)"));
            lore.add(plugin.parseMessage("<!italic><light_purple>Global: <gray>" + numberFormat.format(global) + " <dark_gray>(10 QP each)"));
            lore.add(Component.empty());
            lore.add(plugin.parseMessage("<!italic><gray>Total Earned: <light_purple>" + numberFormat.format(totalEarned) + " QP"));
            lore.add(Component.empty());
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createHowToEarnItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.parseMessage("<yellow><bold>How to Earn QP"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(plugin.parseMessage("<!italic><gray>Complete quests to earn:"));
            lore.add(plugin.parseMessage("<!italic><yellow> • Daily: <light_purple>1 QP"));
            lore.add(plugin.parseMessage("<!italic><gold> • Weekly: <light_purple>5 QP"));
            lore.add(plugin.parseMessage("<!italic><light_purple> • Global: <light_purple>10 QP"));
            lore.add(Component.empty());
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createShopButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.parseMessage("<green><bold>Quest Shop"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(plugin.parseMessage("<!italic><gray>Spend your Quest Points"));
            lore.add(plugin.parseMessage("<!italic><gray>on exclusive rewards!"));
            lore.add(Component.empty());
            lore.add(plugin.parseMessage("<!italic><green>Click to browse"));
            lore.add(Component.empty());
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.parseMessage("<red><bold>← Back"));
            
            List<Component> lore = Arrays.asList(
                Component.empty(),
                plugin.parseMessage("<!italic><gray>Return to main quest menu"),
                Component.empty()
            );
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private int getPlayerTotalQuests(Player player) {
        try {
            int total = 0;
            // Count all completed quests from all managers
            total += plugin.getDailyQuestManager().getPlayerDailyQuests(player.getUniqueId())
                    .stream().filter(q -> q.isClaimed()).count();
            total += plugin.getWeeklyQuestManager().getPlayerWeeklyQuests(player.getUniqueId())
                    .stream().filter(q -> q.isClaimed()).count();
            // Global quests claimed by this player
            total += plugin.getGlobalQuestManager().getActiveGlobalQuests()
                    .stream().filter(q -> q.hasPlayerClaimed(player.getUniqueId())).count();
            return (int) total;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getPlayerDailyCount(Player player) {
        try {
            return (int) plugin.getDailyQuestManager().getPlayerDailyQuests(player.getUniqueId())
                    .stream().filter(q -> q.isClaimed()).count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getPlayerWeeklyCount(Player player) {
        try {
            return (int) plugin.getWeeklyQuestManager().getPlayerWeeklyQuests(player.getUniqueId())
                    .stream().filter(q -> q.isClaimed()).count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getPlayerGlobalCount(Player player) {
        try {
            return (int) plugin.getGlobalQuestManager().getActiveGlobalQuests()
                    .stream().filter(q -> q.hasPlayerClaimed(player.getUniqueId())).count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String getQuestPointsBalance(Player player) {
        try {
            // Try to use CoinsEngine API via reflection
            if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
                try {
                    Class<?> coinsEngineAPI = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
                    
                    // Get the plugin instance method
                    java.lang.reflect.Method getPluginMethod = coinsEngineAPI.getMethod("getPluginInstance");
                    Object coinsEngineInstance = getPluginMethod.invoke(null);
                    
                    // Get user manager
                    java.lang.reflect.Method getUserManagerMethod = coinsEngineInstance.getClass().getMethod("getUserManager");
                    Object userManager = getUserManagerMethod.invoke(coinsEngineInstance);
                    
                    // Get user data
                    java.lang.reflect.Method getUserDataMethod = userManager.getClass().getMethod("getUserData", Player.class);
                    Object userData = getUserDataMethod.invoke(userManager, player);
                    
                    if (userData != null) {
                        // Get currency manager
                        java.lang.reflect.Method getCurrencyManagerMethod = coinsEngineInstance.getClass().getMethod("getCurrencyManager");
                        Object currencyManager = getCurrencyManagerMethod.invoke(coinsEngineInstance);
                        
                        // Get QP currency
                        java.lang.reflect.Method getCurrencyMethod = currencyManager.getClass().getMethod("getCurrency", String.class);
                        Object qpCurrency = getCurrencyMethod.invoke(currencyManager, "qp");
                        
                        if (qpCurrency != null) {
                            // Get balance
                            java.lang.reflect.Method getBalanceMethod = userData.getClass().getMethod("getBalance", qpCurrency.getClass());
                            double balance = (Double) getBalanceMethod.invoke(userData, qpCurrency);
                            return numberFormat.format((int) balance);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to get QP balance via CoinsEngine API: " + e.getMessage());
                }
            }
            
            // Fallback to placeholder
            return "0";
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting QP balance: " + e.getMessage());
            return "Unknown";
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getPlayer() {
        return player;
    }
}
