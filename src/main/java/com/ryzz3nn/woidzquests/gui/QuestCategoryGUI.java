package com.ryzz3nn.woidzquests.gui;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.DailyQuest;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class QuestCategoryGUI implements InventoryHolder {
    
    private final WoidZQuests plugin;
    private final Player player;
    private final Inventory inventory;
    private final QuestCategory category;
    
    public enum QuestCategory {
        DAILY,
        WEEKLY, 
        GLOBAL
    }
    
    public QuestCategoryGUI(WoidZQuests plugin, Player player, QuestCategory category) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
        
        // Get title from messages.yml based on category
        String titleKey = switch (category) {
            case DAILY -> "gui.titles.daily-quests";
            case WEEKLY -> "gui.titles.weekly-quests";
            case GLOBAL -> "gui.titles.global-quests";
        };
        
        String title = plugin.getMessagesManager().getMessage(titleKey);
        this.inventory = Bukkit.createInventory(this, 27, plugin.parseMessage(title));
        setupGUI();
    }
    
    private void setupGUI() {
        // Fill with glass panes
        fillWithGlass();
        
        // Add quest items based on category
        addQuestItems();
        
        // Add back button in bottom middle (slot 22)
        addBackButton();
    }
    
    private void fillWithGlass() {
        // Use centralized filler item (supports Nexo custom items)
        ItemStack glass = plugin.getFillerItem();
        
        // Fill border and empty slots
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, glass);
            }
        }
        
        // Fill remaining empty slots except quest slots and back button
        for (int i = 9; i < 18; i++) {
            if (i != 11 && i != 13 && i != 15) {
                inventory.setItem(i, glass);
            }
        }
        
        // Fill bottom row except back button slot
        for (int i = 18; i < 27; i++) {
            if (i != 22) {
                inventory.setItem(i, glass);
            }
        }
    }
    
    private void addQuestItems() {
        switch (category) {
            case DAILY -> addDailyQuests();
            case WEEKLY -> addWeeklyQuests();
            case GLOBAL -> addGlobalQuests();
        }
    }
    
    private void addDailyQuests() {
        try {
            List<DailyQuest> dailyQuests = plugin.getDailyQuestManager().getPlayerDailyQuests(player.getUniqueId());
            
            if (dailyQuests == null) {
                plugin.getLogger().warning("Daily quests list is null for player: " + player.getName());
                dailyQuests = new ArrayList<>();
            }
            
            int[] slots = {11, 13, 15};
            
            for (int i = 0; i < slots.length; i++) {
                if (i < dailyQuests.size()) {
                    DailyQuest quest = dailyQuests.get(i);
                    try {
                        ItemStack questItem = createDailyQuestItem(quest);
                        inventory.setItem(slots[i], questItem);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error creating daily quest item: " + e.getMessage());
                        e.printStackTrace();
                        // Set an error placeholder
                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add("<!italic><red>Error loading quest");
                        ItemStack errorSlot = createItem(Material.BARRIER, "<!italic><red>Error", lore);
                        inventory.setItem(slots[i], errorSlot);
                    }
                } else {
                    // Empty slot
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add("<!italic><gray>No quest available");
                    lore.add("<!italic><gray>Check back tomorrow for new quests!");
                    
                    ItemStack emptySlot = createItem(Material.GRAY_STAINED_GLASS_PANE, "<!italic><dark_gray>Empty Slot", lore);
                    inventory.setItem(slots[i], emptySlot);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error adding daily quests to GUI: " + e.getMessage());
            e.printStackTrace();
            
            // Fill with error items
            int[] slots = {11, 13, 15};
            for (int slot : slots) {
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("<!italic><red>Error loading daily quests");
                lore.add("<!italic><gray>Check console for details");
                ItemStack errorSlot = createItem(Material.BARRIER, "<!italic><red>Error", lore);
                inventory.setItem(slot, errorSlot);
            }
        }
    }
    
    private ItemStack createDailyQuestItem(DailyQuest quest) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        // Safely get description
        String description = quest.getDescription() != null ? quest.getDescription() : "No description";
        lore.add("<!italic><#E64703>┏ <gray><#f79459>" + description);
        lore.add("<!italic><#E64703>| <gray>Progress: <#f79459>" + quest.getCurrentProgress() + "<gray>/<#f79459>" + quest.getTargetAmount() + " <gray>(" + quest.getProgressColor() + quest.getProgressPercentage() + "%<gray>)");
        
        // Reward information
        DailyQuest.DailyReward reward = quest.getReward();
        if (reward != null) {
            StringBuilder rewardText = new StringBuilder();
            if (reward.getMoney() > 0) {
                rewardText.append("<#f79459>$").append(reward.getMoney());
            }
            if (reward.getExperience() > 0) {
                if (rewardText.length() > 0) rewardText.append(" <gray>+ ");
                rewardText.append("<#f79459>").append(reward.getExperience()).append(" XP");
            }
            
            if (rewardText.length() > 0) {
                lore.add("<!italic><#E64703>| <gray>Reward: " + rewardText.toString());
            }
        }
        
        lore.add("<!italic><#E64703>┗ <gray>Resets at midnight");
        lore.add("");
        
        if (quest.canClaim()) {
            lore.add("<!italic><green>Click to claim your reward!");
        } else if (quest.isClaimed()) {
            lore.add("<!italic><gold>Reward already claimed!");
        } else {
            lore.add("<!italic><gray>Complete the quest to claim rewards!");
        }
        
        // Determine item color based on status
        String nameColor;
        if (quest.isClaimed()) {
            nameColor = "<dark_gray>";
        } else if (quest.canClaim()) {
            nameColor = "<green><bold>";
        } else {
            nameColor = "<yellow><bold>";
        }
        
        // Safely get display material (fallback to STONE if null)
        Material displayMaterial = quest.getDisplayMaterial() != null ? quest.getDisplayMaterial() : Material.STONE;
        
        // Safely get quest name
        String questName = quest.getName() != null ? quest.getName() : "Unknown Quest";
        
        return createItem(displayMaterial, nameColor + questName, lore);
    }
    
    private ItemStack createWeeklyQuestItem(com.ryzz3nn.woidzquests.models.WeeklyQuest quest) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<!italic><#E64703>┏ <gray><#f79459>" + quest.getDescription());
        lore.add("<!italic><#E64703>| <gray>Progress: <#f79459>" + quest.getCurrentProgress() + "<gray>/<#f79459>" + quest.getTargetAmount() + " <gray>(" + quest.getProgressColor() + quest.getProgressPercentage() + "%<gray>)");
        
        // Reward information
        com.ryzz3nn.woidzquests.models.WeeklyQuest.WeeklyReward reward = quest.getReward();
        StringBuilder rewardText = new StringBuilder();
        if (reward.getMoney() > 0) {
            rewardText.append("<#f79459>$").append(reward.getMoney());
        }
        if (reward.getExperience() > 0) {
            if (rewardText.length() > 0) rewardText.append(" <gray>+ ");
            rewardText.append("<#f79459>").append(reward.getExperience()).append(" XP");
        }
        if (reward.getQuestPoints() > 0) {
            if (rewardText.length() > 0) rewardText.append(" <gray>+ ");
            rewardText.append("<#f79459>").append(reward.getQuestPoints()).append(" QP");
        }
        
        lore.add("<!italic><#E64703>| <gray>Reward: " + rewardText.toString());
        lore.add("<!italic><#E64703>┗ <gray>Resets every Monday");
        lore.add("");
        
        if (quest.canClaim()) {
            lore.add("<!italic><green>Click to claim your reward!");
        } else if (quest.isClaimed()) {
            lore.add("<!italic><gold>Reward already claimed!");
        } else {
            lore.add("<!italic><gray>Complete the quest to claim rewards!");
        }
        
        // Determine item color based on status
        String nameColor;
        if (quest.isClaimed()) {
            nameColor = "<dark_gray>";
        } else if (quest.canClaim()) {
            nameColor = "<green><bold>";
        } else {
            nameColor = "<yellow><bold>";
        }
        
        return createItem(quest.getDisplayMaterial(), nameColor + quest.getName(), lore);
    }
    
    private ItemStack createGlobalQuestItem(com.ryzz3nn.woidzquests.models.GlobalQuest quest) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<!italic><#E64703>┏ <gray><#f79459>" + quest.getDescription());
        lore.add("<!italic><#E64703>| <gray>Progress: <#f79459>" + quest.getFormattedProgress() + "<gray>/<#f79459>" + quest.getFormattedTarget() + " <gray>(" + quest.getProgressColor() + quest.getProgressPercentage() + "%<gray>)");
        
        // Show player contribution
        long playerContribution = quest.getPlayerContribution(player.getUniqueId());
        if (playerContribution > 0) {
            String formattedContribution = formatLargeNumber(playerContribution);
            lore.add("<!italic><#E64703>| <gray>Your contribution: <#f79459>" + formattedContribution + "<gray>");
        }
        
        // Reward information
        com.ryzz3nn.woidzquests.models.GlobalQuest.GlobalReward reward = quest.getReward();
        StringBuilder rewardText = new StringBuilder();
        if (reward.getMoney() > 0) {
            rewardText.append("<#f79459>$").append(reward.getMoney());
        }
        if (reward.getExperience() > 0) {
            if (rewardText.length() > 0) rewardText.append(" <gray>+ ");
            rewardText.append("<#f79459>").append(reward.getExperience()).append(" XP");
        }
        if (reward.getQuestPoints() > 0) {
            if (rewardText.length() > 0) rewardText.append(" <gray>+ ");
            rewardText.append("<#f79459>").append(reward.getQuestPoints()).append(" QP");
        }
        
        lore.add("<!italic><#E64703>| <gray>Reward: " + rewardText.toString());
        lore.add("<!italic><#E64703>┗ <gray>Everyone gets rewards when completed!");
        lore.add("");
        
        if (quest.isCompleted() && !quest.hasPlayerClaimed(player.getUniqueId())) {
            if (playerContribution > 0) {
                lore.add("<!italic><green>Click to claim your reward!");
            } else {
                lore.add("<!italic><red>You must contribute to claim rewards!");
            }
        } else if (quest.hasPlayerClaimed(player.getUniqueId())) {
            lore.add("<!italic><gold>Reward already claimed!");
        } else {
            lore.add("<!italic><gray>Contribute to help complete this quest!");
        }
        
        // Determine item color based on status
        String nameColor;
        if (quest.hasPlayerClaimed(player.getUniqueId())) {
            nameColor = "<dark_gray>";
        } else if (quest.isCompleted() && playerContribution > 0) {
            nameColor = "<green><bold>";
        } else if (quest.isCompleted()) {
            nameColor = "<red><bold>";
        } else {
            nameColor = "<light_purple><bold>";
        }
        
        return createItem(quest.getDisplayMaterial(), nameColor + quest.getName(), lore);
    }
    
    private String formatLargeNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
    
    private void addWeeklyQuests() {
        List<com.ryzz3nn.woidzquests.models.WeeklyQuest> weeklyQuests = plugin.getWeeklyQuestManager().getPlayerWeeklyQuests(player.getUniqueId());
        int[] slots = {11, 13, 15}; // Quest slots
        
        for (int i = 0; i < slots.length; i++) {
            if (i < weeklyQuests.size()) {
                com.ryzz3nn.woidzquests.models.WeeklyQuest quest = weeklyQuests.get(i);
                ItemStack questItem = createWeeklyQuestItem(quest);
                inventory.setItem(slots[i], questItem);
            } else {
                // Empty slot
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("<!italic><gray>No quest available");
                lore.add("<!italic><gray>Check back next Monday for new quests!");
                
                ItemStack emptySlot = createItem(Material.GRAY_STAINED_GLASS_PANE, "<!italic><dark_gray>Empty Slot", lore);
                inventory.setItem(slots[i], emptySlot);
            }
        }
    }
    
    private void addGlobalQuests() {
        List<com.ryzz3nn.woidzquests.models.GlobalQuest> globalQuests = plugin.getGlobalQuestManager().getActiveGlobalQuests();
        int[] slots = {11, 13, 15}; // Quest slots
        
        for (int i = 0; i < slots.length; i++) {
            if (i < globalQuests.size()) {
                com.ryzz3nn.woidzquests.models.GlobalQuest quest = globalQuests.get(i);
                ItemStack questItem = createGlobalQuestItem(quest);
                inventory.setItem(slots[i], questItem);
            } else {
                // Empty slot
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("<!italic><gray>No global quest available");
                lore.add("<!italic><gray>Check back later for server-wide challenges!");
                
                ItemStack emptySlot = createItem(Material.GRAY_STAINED_GLASS_PANE, "<!italic><dark_gray>Empty Slot", lore);
                inventory.setItem(slots[i], emptySlot);
            }
        }
    }
    
    private void addBackButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<!italic><gray>Click to return to the main quest menu");
        
        ItemStack backButton = createItem(Material.BARRIER, "<red><bold>← Back", lore);
        inventory.setItem(22, backButton);
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(plugin.parseMessage(name));
            
            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(plugin.parseMessage(line));
            }
            meta.lore(componentLore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public QuestCategory getCategory() {
        return category;
    }
}
