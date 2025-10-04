package com.ryzz3nn.woidzquests.models;

import lombok.Data;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

@Data
public class ShopItem {
    private final int slot;
    private final Material material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final int cost;
    private final List<String> commands;
    private final Map<String, Integer> purchaseLimits; // limitType -> maxPurchases
    
    public ShopItem(int slot, Material material, int amount, String name, List<String> lore, 
                   int cost, List<String> commands, Map<String, Integer> purchaseLimits) {
        this.slot = slot;
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.cost = cost;
        this.commands = commands;
        this.purchaseLimits = purchaseLimits;
    }
}
