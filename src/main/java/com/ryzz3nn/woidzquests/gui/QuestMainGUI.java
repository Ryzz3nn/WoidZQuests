package com.ryzz3nn.woidzquests.gui;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class QuestMainGUI implements InventoryHolder {
    
    private final WoidZQuests plugin;
    private final Player player;
    private final Inventory inventory;
    
    public QuestMainGUI(WoidZQuests plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        // Get title from messages.yml
        String title = plugin.getMessagesManager().getMessage("gui.titles.main-menu");
        this.inventory = Bukkit.createInventory(this, 27, plugin.parseMessage(title));
        
        setupGUI();
    }
    
    private void setupGUI() {
        // Fill with glass panes
        fillWithGlass();
        
        // Daily Quests Button (Slot 11)
        setDailyQuestsButton();
        
        // Weekly Quests Button (Slot 13)
        setWeeklyQuestsButton();
        
        // Global Quests Button (Slot 15)
        setGlobalQuestsButton();
        
        // Quest Points Button (Slot 26 - bottom right)
        setQuestPointsButton();
        
        // Shop Button (Slot 18 - bottom left)
        setShopButton();
    }
    
    private void fillWithGlass() {
        // Use centralized filler item (supports Nexo custom items)
        ItemStack filler = plugin.getFillerItem();
        
        // Fill border and empty slots
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, filler);
            }
        }
        
        // Fill remaining empty slots except quest buttons and quest points
        for (int i = 9; i < 18; i++) {
            if (i != 11 && i != 13 && i != 15) {
                inventory.setItem(i, filler);
            }
        }
        
        // Fill bottom row except shop (18) and quest points (26) slots
        for (int i = 19; i < 26; i++) {
            inventory.setItem(i, filler);
        }
    }
    
    private void setDailyQuestsButton() {
        String timeUntilReset = getTimeUntilDailyReset();
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<!italic><#E64703>┏ <gray><#f79459>Daily Challenges");
        lore.add("<!italic><#E64703>| <gray>Reset: <#f79459>Every day at midnight");
        lore.add("<!italic><#E64703>| <gray>Next reset: <#f79459>" + timeUntilReset);
        lore.add("<!italic><#E64703>| <gray>Reward: <#f79459>1 Quest Point");
        lore.add("<!italic><#E64703>┗ <gray>Moderate rewards & quick completion");
        lore.add("");
        lore.add("<!italic><gray>Click to view your daily quests!");
        
        ItemStack item = createItem(Material.CLOCK, "<yellow><bold>Daily Quests", lore);
        inventory.setItem(11, item);
    }
    
    private void setWeeklyQuestsButton() {
        String timeUntilReset = getTimeUntilWeeklyReset();
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<!italic><#E64703>┏ <gray><#f79459>Weekly Objectives");
        lore.add("<!italic><#E64703>| <gray>Reset: <#f79459>Every Monday");
        lore.add("<!italic><#E64703>| <gray>Next reset: <#f79459>" + timeUntilReset);
        lore.add("<!italic><#E64703>| <gray>Reward: <#f79459>5 Quest Points");
        lore.add("<!italic><#E64703>┗ <gray>Better rewards & quest points");
        lore.add("");
        lore.add("<!italic><gray>Click to view your weekly quests!");
        
        ItemStack item = createItem(Material.BOOK, "<aqua><bold>Weekly Quests", lore);
        inventory.setItem(13, item);
    }
    
    private void setGlobalQuestsButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<!italic><#E64703>┏ <gray><#f79459>Server-Wide Goals");
        lore.add("<!italic><#E64703>| <gray>Type: <#f79459>Collaborative quests");
        lore.add("<!italic><#E64703>| <gray>Refill: <#f79459>30 min after completion");
        lore.add("<!italic><#E64703>| <gray>Reward: <#f79459>10 Quest Points");
        lore.add("<!italic><#E64703>┗ <gray>Massive rewards for everyone");
        lore.add("");
        lore.add("<!italic><gray>Click to view global server quests!");
        
        ItemStack item = createItem(Material.BEACON, "<light_purple><bold>Global Quests", lore);
        inventory.setItem(15, item);
    }
    
    private void setQuestPointsButton() {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int questPoints = playerData.getQuestPoints();
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<!italic><#E64703>┏ <gray><#f79459>Quest Points System");
        lore.add("<!italic><#E64703>| <gray>Current: <#f79459>" + formatNumber(questPoints) + " <gray>points");
        lore.add("<!italic><#E64703>| <gray>Earned from: <#f79459>Weekly & Global quests");
        lore.add("<!italic><#E64703>| <gray>Use for: <#f79459>Exclusive rewards");
        lore.add("<!italic><#E64703>┗ <gray>Coming soon: Quest Point Shop");
        lore.add("");
        lore.add("<!italic><gray>Quest Points are earned from completing");
        lore.add("<!italic><gray>weekly and global quests!");
        lore.add("");
        lore.add("<!italic><dark_gray>Feature coming soon...");
        
        ItemStack item = createItem(Material.NETHER_STAR, "<gold><bold>Quest Points", lore);
        inventory.setItem(26, item);
    }
    
    private void setShopButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<!italic><#E64703>┏ <gray><#f79459>Quest Points Shop");
        lore.add("<!italic><#E64703>| <gray>Spend your <#f79459>Quest Points");
        lore.add("<!italic><#E64703>| <gray>on exclusive items!");
        lore.add("<!italic><#E64703>┗ <gray>Diamonds, tools, keys & more!");
        lore.add("");
        lore.add("<!italic><yellow>Available Categories:");
        lore.add("<!italic><gray>• <aqua>Resources & Materials");
        lore.add("<!italic><gray>• <green>Tools & Equipment");
        lore.add("<!italic><gray>• <gold>Special Items & Keys");
        lore.add("<!italic><gray>• <light_purple>Rare & Premium Items");
        lore.add("");
        lore.add("<!italic><green>Click to browse the shop!");
        
        ItemStack item = createItem(Material.EMERALD, "<green><bold>Quest Points Shop", lore);
        inventory.setItem(18, item);
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
    
    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        } else {
            return String.valueOf(number);
        }
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
    
    private String getTimeUntilDailyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        long hours = ChronoUnit.HOURS.between(now, nextMidnight);
        long minutes = ChronoUnit.MINUTES.between(now, nextMidnight) % 60;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    private String getTimeUntilWeeklyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMonday = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                                      .withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        long days = ChronoUnit.DAYS.between(now, nextMonday);
        long hours = ChronoUnit.HOURS.between(now, nextMonday) % 24;
        
        if (days > 0) {
            return days + "d " + hours + "h";
        } else {
            return hours + "h";
        }
    }
}
