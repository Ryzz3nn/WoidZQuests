package com.ryzz3nn.woidzquests.integrations;

import com.ryzz3nn.woidzquests.WoidZQuests;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

public class VaultIntegration {
    
    private final WoidZQuests plugin;
    private Economy economy;
    private boolean enabled = false;
    
    public VaultIntegration(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                plugin.getLogger().warning("No economy provider found!");
                return false;
            }
            
            economy = rsp.getProvider();
            enabled = true;
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize Vault integration", e);
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled && economy != null;
    }
    
    public boolean hasAccount(Player player) {
        return isEnabled() && economy.hasAccount(player);
    }
    
    public double getBalance(Player player) {
        if (!isEnabled()) return 0.0;
        return economy.getBalance(player);
    }
    
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public boolean deposit(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    public String format(double amount) {
        if (!isEnabled()) return String.valueOf(amount);
        return economy.format(amount);
    }
    
    public String getCurrencyName() {
        if (!isEnabled()) return "coins";
        return economy.currencyNamePlural();
    }
}
