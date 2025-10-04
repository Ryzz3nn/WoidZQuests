package com.ryzz3nn.woidzquests.gui;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.DailyQuest;
import com.ryzz3nn.woidzquests.models.WeeklyQuest;
import com.ryzz3nn.woidzquests.models.GlobalQuest;
import com.ryzz3nn.woidzquests.models.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestGUIListener implements Listener {
    
    private final WoidZQuests plugin;
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
    private static final long CLICK_COOLDOWN_MS = 500; // 500ms cooldown
    
    public QuestGUIListener(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // Only apply cooldown and handle clicks for our GUI inventories
        if (!(holder instanceof QuestMainGUI || holder instanceof QuestCategoryGUI || 
              holder instanceof QuestPointsGUI || holder instanceof QuestPointsShopGUI)) {
            return; // Not our GUI, don't interfere
        }
        
        // Check for click cooldown to prevent double-clicking (only for our GUIs)
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastClick = clickCooldowns.get(playerId);
        
        if (lastClick != null && (currentTime - lastClick) < CLICK_COOLDOWN_MS) {
            event.setCancelled(true);
            return;
        }
        
        clickCooldowns.put(playerId, currentTime);
        
        if (holder instanceof QuestMainGUI mainGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
                return;
            }
            
            int slot = event.getSlot();
            
            switch (slot) {
                case 11 -> {
                    // Daily Quests
                    openDailyQuests(player);
                }
                case 13 -> {
                    // Weekly Quests  
                    openWeeklyQuests(player);
                }
                case 15 -> {
                    // Global Quests
                    openGlobalQuests(player);
                }
                case 18 -> {
                    // Shop
                    openShop(player);
                }
                case 26 -> {
                    // Quest Points
                    openQuestPointsInfo(player);
                }
            }
        }
        
        // Handle other GUI types here when implemented
        else if (holder instanceof QuestCategoryGUI categoryGUI) {
            event.setCancelled(true);
            handleCategoryGUIClick(event, categoryGUI);
        } else if (holder instanceof QuestPointsGUI questPointsGUI) {
            event.setCancelled(true);
            handleQuestPointsGUIClick(event, questPointsGUI);
        } else if (holder instanceof QuestPointsShopGUI shopGUI) {
            event.setCancelled(true);
            handleShopGUIClick(event, shopGUI);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Handle any cleanup if needed
    }
    
    private void openDailyQuests(Player player) {
        QuestCategoryGUI dailyGUI = new QuestCategoryGUI(plugin, player, QuestCategoryGUI.QuestCategory.DAILY);
        dailyGUI.open();
    }
    
    private void openWeeklyQuests(Player player) {
        QuestCategoryGUI weeklyGUI = new QuestCategoryGUI(plugin, player, QuestCategoryGUI.QuestCategory.WEEKLY);
        weeklyGUI.open();
    }
    
    private void openGlobalQuests(Player player) {
        QuestCategoryGUI globalGUI = new QuestCategoryGUI(plugin, player, QuestCategoryGUI.QuestCategory.GLOBAL);
        globalGUI.open();
    }
    
    private void openShop(Player player) {
        QuestPointsShopGUI shopGUI = new QuestPointsShopGUI(plugin, player);
        player.openInventory(shopGUI.getInventory());
    }
    
    private void openQuestPointsInfo(Player player) {
        QuestPointsGUI questPointsGUI = new QuestPointsGUI(plugin, player);
        player.openInventory(questPointsGUI.getInventory());
    }
    
    private void handleCategoryGUIClick(InventoryClickEvent event, QuestCategoryGUI categoryGUI) {
        if (event.getCurrentItem() == null) {
            return;
        }
        
        Player player = categoryGUI.getPlayer();
        int slot = event.getSlot();
        
        if (slot == 22) {
            // Back button clicked
            QuestMainGUI mainGUI = new QuestMainGUI(plugin, player);
            mainGUI.open();
            return;
        }
        
        // Handle quest item clicks (slots 11, 13, 15)
        if (slot == 11 || slot == 13 || slot == 15) {
            handleQuestClick(player, categoryGUI.getCategory(), slot);
        }
    }
    
    private void handleQuestClick(Player player, QuestCategoryGUI.QuestCategory category, int slot) {
        switch (category) {
            case DAILY -> handleDailyQuestClick(player, slot);
            case WEEKLY -> handleWeeklyQuestClick(player, slot);
            case GLOBAL -> handleGlobalQuestClick(player, slot);
        }
    }
    
    private void handleDailyQuestClick(Player player, int slot) {
        List<DailyQuest> dailyQuests = plugin.getDailyQuestManager().getPlayerDailyQuests(player.getUniqueId());
        
        int questIndex = switch (slot) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            default -> -1;
        };
        
        if (questIndex >= 0 && questIndex < dailyQuests.size()) {
            DailyQuest quest = dailyQuests.get(questIndex);
            
            if (quest.canClaim()) {
                // Attempt to claim the reward
                if (plugin.getDailyQuestManager().claimReward(player.getUniqueId(), quest.getId())) {
                    // Refresh the GUI to show updated status
                    QuestCategoryGUI dailyGUI = new QuestCategoryGUI(plugin, player, QuestCategoryGUI.QuestCategory.DAILY);
                    dailyGUI.open();
                }
            } else if (quest.isClaimed()) {
                player.sendMessage(plugin.parseMessage("<gray>[<gold><bold>!</bold></gold><gray>]<reset> You have already claimed the reward for this quest!"));
            } else {
                // Show quest progress
                player.sendMessage(plugin.parseMessage("<gray>[<yellow><bold>◆</bold></yellow><gray>]<reset> Quest: <yellow>" + quest.getName() + "<reset>"));
                player.sendMessage(plugin.parseMessage("<gray>Progress: <yellow>" + quest.getCurrentProgress() + "</yellow><gray>/<yellow>" + quest.getTargetAmount() + "</yellow> <gray>(" + quest.getProgressColor() + quest.getProgressPercentage() + "%<gray>)<reset>"));
                player.sendMessage(plugin.parseMessage("<gray>Complete the quest to claim your reward!<reset>"));
            }
        }
    }
    
    private void handleWeeklyQuestClick(Player player, int slot) {
        List<com.ryzz3nn.woidzquests.models.WeeklyQuest> weeklyQuests = plugin.getWeeklyQuestManager().getPlayerWeeklyQuests(player.getUniqueId());
        
        int questIndex = switch (slot) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            default -> -1;
        };
        
        if (questIndex >= 0 && questIndex < weeklyQuests.size()) {
            com.ryzz3nn.woidzquests.models.WeeklyQuest quest = weeklyQuests.get(questIndex);
            
            if (quest.canClaim()) {
                // Attempt to claim the reward
                if (plugin.getWeeklyQuestManager().claimReward(player.getUniqueId(), quest.getId())) {
                    // Refresh the GUI to show updated status
                    QuestCategoryGUI weeklyGUI = new QuestCategoryGUI(plugin, player, QuestCategoryGUI.QuestCategory.WEEKLY);
                    weeklyGUI.open();
                }
            } else if (quest.isClaimed()) {
                player.sendMessage(plugin.parseMessage("<gray>[<gold><bold>!</bold></gold><gray>]<reset> You have already claimed the reward for this quest!"));
            } else {
                // Show quest progress
                player.sendMessage(plugin.parseMessage("<gray>[<yellow><bold>◆</bold></yellow><gray>]<reset> Quest: <yellow>" + quest.getName() + "<reset>"));
                player.sendMessage(plugin.parseMessage("<gray>Progress: <yellow>" + quest.getCurrentProgress() + "</yellow><gray>/<yellow>" + quest.getTargetAmount() + "</yellow> <gray>(" + quest.getProgressColor() + quest.getProgressPercentage() + "%<gray>)<reset>"));
                player.sendMessage(plugin.parseMessage("<gray>Complete the quest to claim your reward!<reset>"));
            }
        }
    }
    
    private void handleGlobalQuestClick(Player player, int slot) {
        List<com.ryzz3nn.woidzquests.models.GlobalQuest> globalQuests = plugin.getGlobalQuestManager().getActiveGlobalQuests();
        
        int questIndex = switch (slot) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            default -> -1;
        };
        
        if (questIndex >= 0 && questIndex < globalQuests.size()) {
            com.ryzz3nn.woidzquests.models.GlobalQuest quest = globalQuests.get(questIndex);
            
            if (quest.isCompleted() && !quest.hasPlayerClaimed(player.getUniqueId())) {
                // Attempt to claim the reward
                if (plugin.getGlobalQuestManager().claimReward(player.getUniqueId(), quest.getId())) {
                    // Refresh the GUI to show updated status
                    QuestCategoryGUI globalGUI = new QuestCategoryGUI(plugin, player, QuestCategoryGUI.QuestCategory.GLOBAL);
                    globalGUI.open();
                }
            } else if (quest.hasPlayerClaimed(player.getUniqueId())) {
                player.sendMessage(plugin.parseMessage("<gray>[<gold><bold>!</bold></gold><gray>]<reset> You have already claimed the reward for this quest!"));
            } else {
                // Show quest progress and contribution
                long playerContribution = quest.getPlayerContribution(player.getUniqueId());
                player.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>◆</bold></light_purple><gray>]<reset> Global Quest: <light_purple>" + quest.getName() + "<reset>"));
                player.sendMessage(plugin.parseMessage("<gray>Server Progress: <light_purple>" + quest.getFormattedProgress() + "</light_purple><gray>/<light_purple>" + quest.getFormattedTarget() + "</light_purple> <gray>(" + quest.getProgressColor() + quest.getProgressPercentage() + "%<gray>)<reset>"));
                if (playerContribution > 0) {
                    player.sendMessage(plugin.parseMessage("<gray>Your Contribution: <gold>" + formatLargeNumber(playerContribution) + "<reset>"));
                } else {
                    player.sendMessage(plugin.parseMessage("<gray>You haven't contributed to this quest yet!<reset>"));
                }
            }
        }
    }
    
    private String formatLargeNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
    
    private String getQuestName(QuestCategoryGUI.QuestCategory category, int slot) {
        return switch (category) {
            case DAILY -> switch (slot) {
                case 11 -> "Daily Coal Hunt";
                case 13 -> "Daily Fishing Trip";
                case 15 -> "Daily Zombie Slayer";
                default -> null;
            };
            case WEEKLY -> switch (slot) {
                case 11 -> "Ore Master";
                case 13 -> "Harvest Master";
                case 15 -> null; // Empty slot
                default -> null;
            };
            case GLOBAL -> switch (slot) {
                case 11 -> "Diamond Rush";
                case 13 -> "Harvest Festival";
                case 15 -> "Monster Purge";
                default -> null;
            };
        };
    }
    
    private void handleQuestPointsGUIClick(InventoryClickEvent event, QuestPointsGUI questPointsGUI) {
        if (event.getCurrentItem() == null) {
            return;
        }
        
        Player player = questPointsGUI.getPlayer();
        int slot = event.getSlot();
        
        if (slot == 13) { // Shop button (center)
            QuestPointsShopGUI shopGUI = new QuestPointsShopGUI(plugin, player);
            player.openInventory(shopGUI.getInventory());
        } else if (slot == 22) { // Back button (bottom center)
            QuestMainGUI mainGUI = new QuestMainGUI(plugin, player);
            player.openInventory(mainGUI.getInventory());
        }
    }
    
    private void handleShopGUIClick(InventoryClickEvent event, QuestPointsShopGUI shopGUI) {
        if (event.getCurrentItem() == null) {
            return;
        }
        
        Player player = shopGUI.getPlayer();
        int slot = event.getSlot();
        
        if (shopGUI.isBackButton(slot)) {
            // Return to Quest Points GUI
            QuestPointsGUI questPointsGUI = new QuestPointsGUI(plugin, player);
            player.openInventory(questPointsGUI.getInventory());
        } else {
            // Check if it's a shop item
            ShopItem shopItem = shopGUI.getShopItemAtSlot(slot);
            if (shopItem != null) {
                handleShopItemPurchase(player, shopItem);
                
                // Refresh the GUI to update purchase status
                QuestPointsShopGUI newShopGUI = new QuestPointsShopGUI(plugin, player);
                player.openInventory(newShopGUI.getInventory());
            }
        }
    }
    
    private void handleShopItemPurchase(Player player, ShopItem shopItem) {
        // Get current balance first
        int playerBalance = getPlayerQuestPoints(player);
        
        // Check if player has enough Quest Points
        if (playerBalance < shopItem.getCost()) {
            String message = plugin.getShopManager().getInsufficientFundsMessage(player, shopItem);
            player.sendMessage(plugin.parseMessage(message));
            return;
        }
        
        // Check if the shop manager allows the purchase (includes limit checks)
        if (!plugin.getShopManager().canPurchase(player, shopItem)) {
            String message = plugin.getShopManager().getPurchaseLimitMessage(player, shopItem);
            if (message != null) {
                player.sendMessage(plugin.parseMessage(message));
            } else {
                player.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> You cannot purchase this item right now."));
            }
            return;
        }
        
        // Attempt purchase
        boolean success = plugin.getShopManager().purchaseItem(player, shopItem);
        if (!success) {
            // This shouldn't happen if checks passed, but just in case
            player.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Purchase failed! Please try again."));
        }
    }
    
    private int getPlayerQuestPoints(Player player) {
        // Use the ShopManager's method to get actual Quest Points balance
        return plugin.getShopManager().getPlayerQuestPoints(player);
    }
    
    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%,d", number);
        } else {
            return String.valueOf(number);
        }
    }
}
