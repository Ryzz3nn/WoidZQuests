package com.ryzz3nn.woidzquests.gui;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.ShopItem;
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
import java.util.Map;

public class QuestPointsShopGUI implements InventoryHolder {
    
    private final WoidZQuests plugin;
    private final Player player;
    private final Inventory inventory;
    
    public QuestPointsShopGUI(WoidZQuests plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        // Create inventory with configurable size and title from messages.yml
        String title = plugin.getMessagesManager().getMessage("gui.titles.shop");
        int size = plugin.getShopManager().getShopSize();
        this.inventory = Bukkit.createInventory(this, size, plugin.parseMessage(title));
        
        setupGUI();
    }
    
    private void setupGUI() {
        // Fill with glass panes if enabled
        if (plugin.getShopManager().shouldFillGlass()) {
            fillWithGlass();
        }
        
        // Add shop items
        addShopItems();
        
        // Add navigation items
        addNavigationItems();
    }
    
    private void fillWithGlass() {
        // Use centralized filler item (supports Nexo custom items)
        ItemStack glass = plugin.getFillerItem();
        
        // Fill all slots with filler
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }
    }
    
    private void addShopItems() {
        Map<Integer, ShopItem> shopItems = plugin.getShopManager().getShopItems();
        
        for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
            int slot = entry.getKey();
            ShopItem shopItem = entry.getValue();
            
            // Validate slot is within inventory bounds
            if (slot >= 0 && slot < inventory.getSize()) {
                ItemStack item = createShopItemStack(shopItem);
                inventory.setItem(slot, item);
            }
        }
    }
    
    private ItemStack createShopItemStack(ShopItem shopItem) {
        ItemStack item = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            if (!shopItem.getName().isEmpty()) {
                meta.displayName(plugin.parseMessage(shopItem.getName()));
            }
            
            // Set lore
            if (!shopItem.getLore().isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String loreLine : shopItem.getLore()) {
                    loreComponents.add(plugin.parseMessage(loreLine));
                }
                
                // Add purchase status information
                addPurchaseStatusToLore(loreComponents, shopItem);
                
                meta.lore(loreComponents);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private void addPurchaseStatusToLore(List<Component> lore, ShopItem shopItem) {
        // Add separator if lore exists
        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }
        
        // Check if player can afford the item
        boolean canAfford = plugin.getShopManager().canPurchase(player, shopItem);
        
        if (canAfford) {
            lore.add(plugin.parseMessage("<!italic><green>✓ You can afford this item!"));
        } else {
            // Check if it's a funds issue or limit issue
            int playerBalance = getPlayerQuestPoints();
            if (playerBalance < shopItem.getCost()) {
                lore.add(plugin.parseMessage("<!italic><red>✗ Insufficient Quest Points"));
                lore.add(plugin.parseMessage("<!italic><gray>Need: <light_purple>" + shopItem.getCost() + " QP"));
                lore.add(plugin.parseMessage("<!italic><gray>Have: <light_purple>" + playerBalance + " QP"));
            } else {
                lore.add(plugin.parseMessage("<!italic><red>✗ Purchase limit reached"));
            }
        }
        
        // Add purchase limits info if they exist
        if (!shopItem.getPurchaseLimits().isEmpty()) {
            lore.add(Component.empty());
            lore.add(plugin.parseMessage("<!italic><yellow>Purchase Limits:"));
            
            for (Map.Entry<String, Integer> limitEntry : shopItem.getPurchaseLimits().entrySet()) {
                String limitType = limitEntry.getKey();
                int maxPurchases = limitEntry.getValue();
                int currentPurchases = getCurrentPurchases(shopItem, limitType);
                
                String limitText = "<!italic><gray>• " + capitalize(limitType) + ": <yellow>" + 
                                 currentPurchases + "/" + maxPurchases;
                lore.add(plugin.parseMessage(limitText));
            }
        }
    }
    
    private int getCurrentPurchases(ShopItem shopItem, String limitType) {
        return plugin.getShopManager().getCurrentPurchases(player, shopItem, limitType);
    }
    
    private int getPlayerQuestPoints() {
        // Use the ShopManager's method to get actual Quest Points balance
        return plugin.getShopManager().getPlayerQuestPoints(player);
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private void addNavigationItems() {
        // Add back button
        int backSlot = plugin.getShopManager().getBackButtonSlot();
        if (backSlot >= 0 && backSlot < inventory.getSize()) {
            ItemStack backButton = createBackButton();
            inventory.setItem(backSlot, backButton);
        }
    }
    
    private ItemStack createBackButton() {
        Material material = plugin.getShopManager().getBackButtonMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = plugin.getShopManager().getBackButtonName();
            meta.displayName(plugin.parseMessage(name));
            
            List<String> loreStrings = plugin.getShopManager().getBackButtonLore();
            if (!loreStrings.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String loreLine : loreStrings) {
                    lore.add(plugin.parseMessage(loreLine));
                }
                meta.lore(lore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public ShopItem getShopItemAtSlot(int slot) {
        return plugin.getShopManager().getShopItems().get(slot);
    }
    
    public boolean isBackButton(int slot) {
        return slot == plugin.getShopManager().getBackButtonSlot();
    }
}
