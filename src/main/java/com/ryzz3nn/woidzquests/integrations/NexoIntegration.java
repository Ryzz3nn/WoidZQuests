package com.ryzz3nn.woidzquests.integrations;

import com.ryzz3nn.woidzquests.WoidZQuests;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Integration for Nexo custom items plugin
 * Supports using custom Nexo items as GUI filler materials
 * Uses reflection to avoid hard dependency - based on WoidZOrders implementation
 */
public class NexoIntegration {
    
    private final WoidZQuests plugin;
    private boolean enabled = false;
    
    public NexoIntegration(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        Plugin nexo = Bukkit.getPluginManager().getPlugin("Nexo");
        if (nexo != null && nexo.isEnabled()) {
            try {
                // Use reflection to access Nexo API without hard dependency
                Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
                
                // Just verify the class exists - we'll try methods dynamically in getItem()
                enabled = true;
                plugin.getLogger().info("Nexo integration enabled - Custom items available!");
                return true;
                
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("Nexo found but API not available. Using standard items.");
                return false;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to initialize Nexo integration", e);
                return false;
            }
        }
        return false;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get a Nexo custom item by its ID
     * Tries multiple API methods for compatibility with different Nexo versions
     * @param itemId The Nexo item ID WITHOUT prefix (e.g., "guiblank" not "nexo:guiblank")
     * @return ItemStack of the custom item, or null if not found
     */
    public ItemStack getItem(String itemId) {
        if (!isEnabled()) {
            return null;
        }
        
        try {
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            
            // Method 1: Try itemFromId (returns builder)
            try {
                Method getItemMethod = nexoItemsClass.getMethod("itemFromId", String.class);
                Object nexoItemBuilder = getItemMethod.invoke(null, itemId);
                
                if (nexoItemBuilder != null) {
                    Method buildMethod = nexoItemBuilder.getClass().getMethod("build");
                    ItemStack nexoItem = (ItemStack) buildMethod.invoke(nexoItemBuilder);
                    
                    if (nexoItem != null) {
                        return nexoItem.clone();
                    }
                }
            } catch (NoSuchMethodException e1) {
                // Method 2: Try direct itemStack method
                try {
                    Method getItemStackMethod = nexoItemsClass.getMethod("itemStack", String.class);
                    ItemStack nexoItem = (ItemStack) getItemStackMethod.invoke(null, itemId);
                    
                    if (nexoItem != null) {
                        return nexoItem.clone();
                    }
                } catch (NoSuchMethodException e2) {
                    // Method 3: Try get method
                    try {
                        Method getMethod = nexoItemsClass.getMethod("get", String.class);
                        ItemStack nexoItem = (ItemStack) getMethod.invoke(null, itemId);
                        
                        if (nexoItem != null) {
                            return nexoItem.clone();
                        }
                    } catch (NoSuchMethodException e3) {
                        plugin.getLogger().warning("Could not find suitable Nexo API method. Tried: itemFromId, itemStack, get");
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get Nexo item: " + itemId, e);
        }
        
        return null;
    }
    
    /**
     * Check if a Nexo item exists
     * @param itemId The Nexo item ID to check
     * @return true if the item exists, false otherwise
     */
    public boolean hasItem(String itemId) {
        return getItem(itemId) != null;
    }
}

