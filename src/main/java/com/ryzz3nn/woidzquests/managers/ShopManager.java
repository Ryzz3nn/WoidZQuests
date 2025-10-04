package com.ryzz3nn.woidzquests.managers;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.ShopItem;
import com.ryzz3nn.woidzquests.models.PurchaseLimit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {
    
    private final WoidZQuests plugin;
    private FileConfiguration shopConfig;
    private final Map<Integer, ShopItem> shopItems = new HashMap<>();
    private final Map<UUID, Map<String, PurchaseLimit>> playerPurchaseLimits = new ConcurrentHashMap<>();
    
    // Shop configuration
    private String shopTitle;
    private int shopSize;
    private String currencyPlaceholder;
    private String currencyName;
    private String currencySymbol;
    private boolean fillGlass;
    private Material glassMaterial;
    
    // Navigation
    private int backButtonSlot;
    private Material backButtonMaterial;
    private String backButtonName;
    private List<String> backButtonLore;
    
    public ShopManager(WoidZQuests plugin) {
        this.plugin = plugin;
        loadShopConfig();
    }
    
    private void loadShopConfig() {
        File shopFile = new File(plugin.getDataFolder(), "shop.yml");
        
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        loadShopSettings();
        loadShopItems();
        loadNavigationItems();
    }
    
    private void loadShopSettings() {
        ConfigurationSection shopSection = shopConfig.getConfigurationSection("shop");
        if (shopSection != null) {
            // GUI settings
            ConfigurationSection guiSection = shopSection.getConfigurationSection("gui");
            if (guiSection != null) {
                shopTitle = guiSection.getString("title", "<light_purple><bold>Quest Points Shop</bold></light_purple>");
                shopSize = guiSection.getInt("size", 54);
                
                // Validate size
                if (shopSize % 9 != 0 || shopSize < 9 || shopSize > 54) {
                    plugin.getLogger().warning("Invalid shop size: " + shopSize + ". Using default size 54.");
                    shopSize = 54;
                }
            }
            
            // Currency settings
            ConfigurationSection currencySection = shopSection.getConfigurationSection("currency");
            if (currencySection != null) {
                currencyPlaceholder = currencySection.getString("placeholder", "%coinsengine_balance_raw_qp%");
                currencyName = currencySection.getString("name", "Quest Points");
                currencySymbol = currencySection.getString("symbol", "QP");
            }
            
            // Glass fill settings
            fillGlass = shopSection.getBoolean("fill_glass", true);
            String glassMaterialName = shopSection.getString("glass_material", "GRAY_STAINED_GLASS_PANE");
            try {
                glassMaterial = Material.valueOf(glassMaterialName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid glass material: " + glassMaterialName + ". Using GRAY_STAINED_GLASS_PANE.");
                glassMaterial = Material.GRAY_STAINED_GLASS_PANE;
            }
        }
    }
    
    private void loadShopItems() {
        shopItems.clear();
        ConfigurationSection itemsSection = shopConfig.getConfigurationSection("items");
        
        if (itemsSection != null) {
            for (String slotStr : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotStr);
                    
                    if (itemSection != null && itemSection.getBoolean("enabled", true)) {
                        ShopItem shopItem = loadShopItem(itemSection, slot);
                        if (shopItem != null) {
                            shopItems.put(slot, shopItem);
                        }
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot number in shop config: " + slotStr);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + shopItems.size() + " shop items");
    }
    
    private ShopItem loadShopItem(ConfigurationSection section, int slot) {
        try {
            String materialName = section.getString("material");
            if (materialName == null) return null;
            
            Material material = Material.valueOf(materialName);
            int amount = section.getInt("amount", 1);
            String name = section.getString("name", "");
            List<String> lore = section.getStringList("lore");
            int cost = section.getInt("cost", 0);
            List<String> commands = section.getStringList("commands");
            
            // Load purchase limits
            Map<String, Integer> limits = new HashMap<>();
            ConfigurationSection limitsSection = section.getConfigurationSection("purchase_limits");
            if (limitsSection != null) {
                for (String limitType : limitsSection.getKeys(false)) {
                    limits.put(limitType, limitsSection.getInt(limitType));
                }
            }
            
            return new ShopItem(slot, material, amount, name, lore, cost, commands, limits);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load shop item at slot " + slot + ": " + e.getMessage());
            return null;
        }
    }
    
    private void loadNavigationItems() {
        ConfigurationSection navSection = shopConfig.getConfigurationSection("navigation.back_button");
        if (navSection != null) {
            backButtonSlot = navSection.getInt("slot", 49);
            String materialName = navSection.getString("material", "BARRIER");
            try {
                backButtonMaterial = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                backButtonMaterial = Material.BARRIER;
            }
            backButtonName = navSection.getString("name", "<red><bold>← Back</bold></red>");
            backButtonLore = navSection.getStringList("lore");
        }
    }
    
    public boolean canPurchase(Player player, ShopItem item) {
        // Check if player has enough quest points
        int playerBalance = getPlayerQuestPoints(player);
        if (playerBalance < item.getCost()) {
            return false;
        }
        
        // Check purchase limits
        return !hasReachedPurchaseLimit(player, item);
    }
    
    public boolean purchaseItem(Player player, ShopItem item) {
        // Double-check balance before purchase
        int currentBalance = getPlayerQuestPoints(player);
        if (currentBalance < item.getCost()) {
            String message = getInsufficientFundsMessage(player, item);
            player.sendMessage(plugin.parseMessage(message));
            return false;
        }
        
        // Check purchase limits
        if (hasReachedPurchaseLimit(player, item)) {
            String message = getPurchaseLimitMessage(player, item);
            if (message != null) {
                player.sendMessage(plugin.parseMessage(message));
            }
            return false;
        }
        
        // Deduct quest points with error handling
        try {
            String deductCommand = "qp take " + player.getName() + " " + item.getCost();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), deductCommand);
            
            // Wait a moment for the command to process
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Verify the deduction worked by checking balance again
            int newBalance = getPlayerQuestPoints(player);
            int expectedBalance = currentBalance - item.getCost();
            if (newBalance != expectedBalance) {
                // Deduction didn't work as expected
                plugin.getLogger().warning("Quest Points deduction verification failed for " + player.getName() + 
                    ". Expected: " + expectedBalance + ", Actual: " + newBalance + ", Original: " + currentBalance);
                player.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Purchase failed! Please try again."));
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deduct Quest Points for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Purchase failed! Could not process Quest Points transaction."));
            return false;
        }
        
        // Execute item commands with error handling
        for (String command : item.getCommands()) {
            try {
                String processedCommand = command.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute shop command '" + command + "' for " + player.getName() + ": " + e.getMessage());
                // Continue with other commands even if one fails
            }
        }
        
        // Update purchase limits
        updatePurchaseLimit(player, item);
        
        // Send success message
        String message = shopConfig.getString("messages.purchase_success", 
            "<gray>[<green><bold>✓</bold></green><gray>]<reset> Successfully purchased <yellow>{item}<reset> for <light_purple>{cost} QP<reset>!")
            .replace("{item}", item.getName())
            .replace("{cost}", String.valueOf(item.getCost()));
        
        player.sendMessage(plugin.parseMessage(message));
        
        return true;
    }
    
    public int getCurrentPurchases(Player player, ShopItem item, String limitType) {
        Map<String, PurchaseLimit> playerLimits = playerPurchaseLimits.get(player.getUniqueId());
        if (playerLimits == null) {
            return 0;
        }
        
        String itemKey = "item_" + item.getSlot();
        String limitKey = itemKey + "_" + limitType;
        PurchaseLimit limit = playerLimits.get(limitKey);
        
        if (limit == null) {
            return 0;
        }
        
        // Check if limit period has expired
        if (hasLimitExpired(limit, limitType)) {
            playerLimits.remove(limitKey); // Reset expired limit
            return 0;
        }
        
        return limit.getPurchaseCount();
    }
    
    private boolean hasReachedPurchaseLimit(Player player, ShopItem item) {
        if (item.getPurchaseLimits().isEmpty()) {
            return false;
        }
        
        Map<String, PurchaseLimit> playerLimits = playerPurchaseLimits.computeIfAbsent(
            player.getUniqueId(), k -> new ConcurrentHashMap<>());
        
        String itemKey = "item_" + item.getSlot();
        
        for (Map.Entry<String, Integer> limitEntry : item.getPurchaseLimits().entrySet()) {
            String limitType = limitEntry.getKey();
            int maxPurchases = limitEntry.getValue();
            
            String limitKey = itemKey + "_" + limitType;
            PurchaseLimit limit = playerLimits.get(limitKey);
            
            if (limit == null) {
                continue; // No purchases yet
            }
            
            // Check if limit period has expired
            if (hasLimitExpired(limit, limitType)) {
                playerLimits.remove(limitKey); // Reset expired limit
                continue;
            }
            
            // Check if limit is reached
            if (limit.getPurchaseCount() >= maxPurchases) {
                return true;
            }
        }
        
        return false;
    }
    
    private void updatePurchaseLimit(Player player, ShopItem item) {
        if (item.getPurchaseLimits().isEmpty()) {
            return;
        }
        
        Map<String, PurchaseLimit> playerLimits = playerPurchaseLimits.computeIfAbsent(
            player.getUniqueId(), k -> new ConcurrentHashMap<>());
        
        String itemKey = "item_" + item.getSlot();
        LocalDateTime now = LocalDateTime.now();
        
        for (String limitType : item.getPurchaseLimits().keySet()) {
            String limitKey = itemKey + "_" + limitType;
            PurchaseLimit limit = playerLimits.get(limitKey);
            
            if (limit == null || hasLimitExpired(limit, limitType)) {
                // Create new limit or reset expired one
                limit = new PurchaseLimit(now, 1);
            } else {
                // Increment existing limit
                limit.incrementPurchases();
            }
            
            playerLimits.put(limitKey, limit);
        }
    }
    
    private boolean hasLimitExpired(PurchaseLimit limit, String limitType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resetTime = limit.getFirstPurchaseTime();
        
        return switch (limitType.toLowerCase()) {
            case "daily" -> ChronoUnit.DAYS.between(resetTime, now) >= 1;
            case "weekly" -> ChronoUnit.WEEKS.between(resetTime, now) >= 1;
            case "monthly" -> ChronoUnit.MONTHS.between(resetTime, now) >= 1;
            default -> false;
        };
    }
    
    public int getPlayerQuestPoints(Player player) {
        // Use PlaceholderAPI to get actual Quest Points balance
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                // Use reflection to avoid compile-time dependency
                Class<?> placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method setPlaceholders = placeholderAPI.getMethod("setPlaceholders", Player.class, String.class);
                String balanceStr = (String) setPlaceholders.invoke(null, player, currencyPlaceholder);
                
                if (balanceStr != null && !balanceStr.equals(currencyPlaceholder)) {
                    // Remove any non-numeric characters except decimal point
                    balanceStr = balanceStr.replaceAll("[^0-9.]", "");
                    if (!balanceStr.isEmpty()) {
                        return (int) Double.parseDouble(balanceStr);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get Quest Points balance for " + player.getName() + ": " + e.getMessage());
            }
        }
        
        // Fallback: For testing purposes, give players a default amount
        // TODO: Server owners should set up PlaceholderAPI with CoinsEngine for proper balance checking
        plugin.getLogger().warning("Quest Points balance check for " + player.getName() + " - PlaceholderAPI not available or placeholder '" + currencyPlaceholder + "' not working. Using fallback value of 50 QP for testing.");
        
        // Return a test amount so the shop can be tested
        return 50;
    }
    
    public String getPurchaseLimitMessage(Player player, ShopItem item) {
        for (Map.Entry<String, Integer> limitEntry : item.getPurchaseLimits().entrySet()) {
            String limitType = limitEntry.getKey();
            
            if (hasReachedPurchaseLimit(player, item)) {
                return shopConfig.getString("messages.purchase_limit_reached",
                    "<gray>[<red><bold>!</bold></red><gray>]<reset> You've reached the {limit_type} purchase limit for this item!")
                    .replace("{limit_type}", limitType);
            }
        }
        return null;
    }
    
    public String getInsufficientFundsMessage(Player player, ShopItem item) {
        int balance = getPlayerQuestPoints(player);
        return shopConfig.getString("messages.insufficient_funds",
            "<gray>[<red><bold>!</bold></red><gray>]<reset> You need <light_purple>{cost} QP<reset> but only have <light_purple>{balance} QP<reset>!")
            .replace("{cost}", String.valueOf(item.getCost()))
            .replace("{balance}", String.valueOf(balance));
    }
    
    // Getters
    public Map<Integer, ShopItem> getShopItems() { return new HashMap<>(shopItems); }
    public String getShopTitle() { return shopTitle; }
    public int getShopSize() { return shopSize; }
    public boolean shouldFillGlass() { return fillGlass; }
    public Material getGlassMaterial() { return glassMaterial; }
    public int getBackButtonSlot() { return backButtonSlot; }
    public Material getBackButtonMaterial() { return backButtonMaterial; }
    public String getBackButtonName() { return backButtonName; }
    public List<String> getBackButtonLore() { return new ArrayList<>(backButtonLore); }
    
    public void reload() {
        loadShopConfig();
    }
}
