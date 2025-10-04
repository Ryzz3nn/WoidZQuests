package com.ryzz3nn.woidzquests;

import com.ryzz3nn.woidzquests.commands.QuestsCommand;
import com.ryzz3nn.woidzquests.commands.WoidZQuestsCommand;
import com.ryzz3nn.woidzquests.config.ConfigManager;
import com.ryzz3nn.woidzquests.config.MessagesManager;
import com.ryzz3nn.woidzquests.database.DatabaseManager;
import com.ryzz3nn.woidzquests.listeners.*;
import com.ryzz3nn.woidzquests.managers.QuestManager;
import com.ryzz3nn.woidzquests.managers.PlayerDataManager;
import com.ryzz3nn.woidzquests.managers.DailyQuestManager;
import com.ryzz3nn.woidzquests.managers.WeeklyQuestManager;
import com.ryzz3nn.woidzquests.managers.GlobalQuestManager;
import com.ryzz3nn.woidzquests.managers.BossBarManager;
import com.ryzz3nn.woidzquests.managers.ShopManager;
import com.ryzz3nn.woidzquests.managers.PlaytimeTracker;
import com.ryzz3nn.woidzquests.integrations.JobsIntegration;
import com.ryzz3nn.woidzquests.integrations.VaultIntegration;
import com.ryzz3nn.woidzquests.integrations.WoidZFishingIntegration;
import com.ryzz3nn.woidzquests.integrations.NexoIntegration;
import com.ryzz3nn.woidzquests.placeholders.QuestPlaceholders;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

@Getter
public final class WoidZQuests extends JavaPlugin {
    
    private static WoidZQuests instance;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    // Managers
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DatabaseManager databaseManager;
    private QuestManager questManager;
    private PlayerDataManager playerDataManager;
    private DailyQuestManager dailyQuestManager;
    private WeeklyQuestManager weeklyQuestManager;
    private GlobalQuestManager globalQuestManager;
    private BossBarManager bossBarManager;
    private ShopManager shopManager;
    private PlaytimeTracker playtimeTracker;
    
    // Integrations
    private VaultIntegration vaultIntegration;
    private JobsIntegration jobsIntegration;
    private WoidZFishingIntegration woidzFishingIntegration;
    private NexoIntegration nexoIntegration;
    private QuestPlaceholders questPlaceholders;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        if (!initializeManagers()) {
            getLogger().severe("Failed to initialize managers! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize integrations
        initializeIntegrations();
        
        // Register listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        getLogger().info("WoidZQuests has been enabled successfully!");
        getLogger().info("Server: WoidZ Dev Server");
        getLogger().info("Version: " + getDescription().getVersion());
    }
    
    @Override
    public void onDisable() {
        // Stop playtime tracking
        if (playtimeTracker != null) {
            playtimeTracker.stopTracking();
        }
        
        // Cleanup boss bars
        if (bossBarManager != null) {
            bossBarManager.cleanup();
        }
        
        // Cleanup global quest refill tasks
        if (globalQuestManager != null) {
            globalQuestManager.cleanup();
        }
        
        // Save all player data
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        
        // Close database connections
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        // Unregister placeholders
        if (questPlaceholders != null) {
            questPlaceholders.unregister();
        }
        
        getLogger().info("WoidZQuests has been disabled!");
    }
    
    private boolean initializeManagers() {
        try {
            // Save all quest configuration files to plugin folder
            saveDefaultQuestConfigs();
            
            // Config Manager
            configManager = new ConfigManager(this);
            if (!configManager.initialize()) {
                return false;
            }
            
            // Messages Manager
            messagesManager = new MessagesManager(this);
            if (!messagesManager.initialize()) {
                return false;
            }
            
            // Database Manager
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                return false;
            }
            
            // Player Data Manager
            playerDataManager = new PlayerDataManager(this);
            
            // Quest Manager
            questManager = new QuestManager(this);
            if (!questManager.initialize()) {
                return false;
            }
            
            // Daily Quest Manager
            dailyQuestManager = new DailyQuestManager(this);
            
            // Weekly Quest Manager
            weeklyQuestManager = new WeeklyQuestManager(this);
            
            // Global Quest Manager
            globalQuestManager = new GlobalQuestManager(this);
            
            // Boss Bar Manager
            bossBarManager = new BossBarManager(this);
            
            // Shop Manager
            shopManager = new ShopManager(this);
            
            // Playtime Tracker
            playtimeTracker = new PlaytimeTracker(this);
            playtimeTracker.startTracking();
            
            // Start anti-cheese cleanup task
            startAntiCheeseCleanup();
            
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing managers", e);
            return false;
        }
    }
    
    private void initializeIntegrations() {
        // Vault Integration
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultIntegration = new VaultIntegration(this);
            if (vaultIntegration.initialize()) {
                getLogger().info("Vault integration enabled!");
            }
        }
        
        // Jobs Integration
        if (Bukkit.getPluginManager().getPlugin("Jobs") != null) {
            jobsIntegration = new JobsIntegration(this);
            if (jobsIntegration.initialize()) {
                getLogger().info("Jobs Reborn integration enabled!");
            }
        }
        
        // WoidZFishing Integration
        woidzFishingIntegration = new WoidZFishingIntegration(this);
        woidzFishingIntegration.initialize();
        
        // Nexo Integration
        if (Bukkit.getPluginManager().getPlugin("Nexo") != null) {
            nexoIntegration = new NexoIntegration(this);
            if (nexoIntegration.initialize()) {
                getLogger().info("Nexo integration enabled - Custom items available!");
            }
        }
        
        // PlaceholderAPI Integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            questPlaceholders = new QuestPlaceholders(this);
            questPlaceholders.register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new com.ryzz3nn.woidzquests.gui.QuestGUIListener(this), this);
        
        if (jobsIntegration != null) {
            getServer().getPluginManager().registerEvents(new JobsListener(this), this);
        }
    }
    
    private void registerCommands() {
        getCommand("quests").setExecutor(new QuestsCommand(this));
        getCommand("woidzquests").setExecutor(new WoidZQuestsCommand(this));
    }
    
    public void reload() {
        try {
            configManager.reload();
            messagesManager.reload();
            questManager.reload();
            getLogger().info("WoidZQuests reloaded successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error reloading plugin", e);
        }
    }
    
    public Component parseMessage(String message) {
        return miniMessage.deserialize(message);
    }
    
    public static WoidZQuests getInstance() {
        return instance;
    }
    
    /**
     * Save all quest configuration files to the plugin folder
     * This ensures users can edit GlobalQuests, WeeklyQuests, and JobQuests on the server
     */
    private void saveDefaultQuestConfigs() {
        // Save quest configuration files if they don't exist
        saveResource("DailyQuests.yml", false);
        saveResource("WeeklyQuests.yml", false);
        saveResource("GlobalQuests.yml", false);
        saveResource("JobQuests.yml", false);
        saveResource("shop.yml", false);
        
        getLogger().info("Quest configuration files saved to plugin folder!");
    }
    
    /**
     * Start periodic cleanup of old placed blocks from anti-cheese system
     * This prevents the database from growing indefinitely
     */
    private void startAntiCheeseCleanup() {
        if (!configManager.getBoolean("quests.anti-cheese.track-placed-blocks", true)) {
            return; // Don't run cleanup if tracking is disabled
        }
        
        // Get cleanup interval from config (default: 60 minutes)
        int cleanupIntervalMinutes = configManager.getInt("quests.anti-cheese.cleanup-interval-minutes", 60);
        long cleanupIntervalTicks = cleanupIntervalMinutes * 60 * 20L; // Convert to ticks
        
        // Get tracking duration from config (default: 24 hours)
        int trackingDurationHours = configManager.getInt("quests.anti-cheese.tracking-duration-hours", 24);
        long trackingDurationMs = trackingDurationHours * 60 * 60 * 1000L;
        
        // Schedule repeating cleanup task
        Bukkit.getScheduler().runTaskTimerAsync(this, () -> {
            databaseManager.executeAsync(connection -> {
                String sql = "DELETE FROM placed_blocks WHERE placed_at < ?";
                
                try (var statement = connection.prepareStatement(sql)) {
                    long cutoffTime = System.currentTimeMillis() - trackingDurationMs;
                    statement.setLong(1, cutoffTime);
                    
                    int deletedCount = statement.executeUpdate();
                    
                    if (deletedCount > 0) {
                        getLogger().info("[Anti-Cheese] Cleaned up " + deletedCount + " old placed blocks");
                    }
                } catch (Exception e) {
                    getLogger().warning("[Anti-Cheese] Failed to clean up old placed blocks: " + e.getMessage());
                }
            });
        }, cleanupIntervalTicks, cleanupIntervalTicks); // Run periodically
        
        getLogger().info("[Anti-Cheese] Cleanup task started (interval: " + cleanupIntervalMinutes + " minutes)");
    }
    
    // Getters for managers
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public DailyQuestManager getDailyQuestManager() {
        return dailyQuestManager;
    }
    
    public WeeklyQuestManager getWeeklyQuestManager() {
        return weeklyQuestManager;
    }
    
    public GlobalQuestManager getGlobalQuestManager() {
        return globalQuestManager;
    }
    
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }
    
    public VaultIntegration getVaultIntegration() {
        return vaultIntegration;
    }
    
    public JobsIntegration getJobsIntegration() {
        return jobsIntegration;
    }
    
    public WoidZFishingIntegration getWoidZFishingIntegration() {
        return woidzFishingIntegration;
    }
    
    public PlaytimeTracker getPlaytimeTracker() {
        return playtimeTracker;
    }
    
    public NexoIntegration getNexoIntegration() {
        return nexoIntegration;
    }
    
    /**
     * Get filler item for GUIs - supports Nexo custom items
     * @return ItemStack for GUI filler (Nexo custom item or vanilla glass pane)
     */
    public ItemStack getFillerItem() {
        // Check if custom filler is enabled
        boolean useCustom = configManager.getBoolean("gui.filler.use-custom-item", false);
        boolean debug = configManager.getBoolean("debug", false);
        
        if (debug) {
            getLogger().info("[DEBUG] Filler Item - Use Custom: " + useCustom);
            getLogger().info("[DEBUG] Filler Item - Nexo Available: " + (nexoIntegration != null && nexoIntegration.isEnabled()));
        }
        
        if (useCustom && nexoIntegration != null && nexoIntegration.isEnabled()) {
            String customItemId = configManager.getString("gui.filler.custom-item-id", "guiblank");
            
            // Remove "nexo:" prefix if present - Nexo API expects just the ID (e.g., "guiblank" not "nexo:guiblank")
            if (customItemId.startsWith("nexo:")) {
                customItemId = customItemId.substring(5);
            }
            
            if (debug) {
                getLogger().info("[DEBUG] Filler Item - Item ID for Nexo API: " + customItemId);
            }
            
            ItemStack customItem = nexoIntegration.getItem(customItemId);
            if (customItem != null) {
                if (debug) {
                    getLogger().info("[DEBUG] Filler Item - Successfully loaded Nexo item: " + customItemId);
                    getLogger().info("[DEBUG] Filler Item - Item Type: " + customItem.getType());
                    if (customItem.hasItemMeta()) {
                        getLogger().info("[DEBUG] Filler Item - Has ItemMeta: true");
                    }
                }
                
                // Remove display name from Nexo item for clean hover
                org.bukkit.inventory.meta.ItemMeta nexoMeta = customItem.getItemMeta();
                if (nexoMeta != null) {
                    nexoMeta.displayName(net.kyori.adventure.text.Component.empty());
                    customItem.setItemMeta(nexoMeta);
                }
                
                return customItem;
            } else {
                getLogger().warning("Nexo item '" + customItemId + "' not found! Using vanilla glass pane.");
                getLogger().warning("Make sure the item exists in your Nexo configuration.");
                getLogger().warning("Available Nexo items can be listed with: /nexo items list");
            }
        }
        
        if (debug) {
            getLogger().info("[DEBUG] Filler Item - Using fallback vanilla glass pane");
        }
        
        // Fallback to vanilla glass pane (no display name for clean hover)
        org.bukkit.inventory.ItemStack glass = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            // Remove display name completely so there's no hover tooltip
            meta.displayName(net.kyori.adventure.text.Component.empty());
            glass.setItemMeta(meta);
        }
        return glass;
    }
}
