package com.ryzz3nn.woidzquests.listeners;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.Quest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;

public class CombatListener implements Listener {
    
    private final WoidZQuests plugin;
    
    public CombatListener(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        if (killer == null) {
            return;
        }
        
        // Create progress data
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("mob_type", entity.getType().name());
        progressData.put("world", entity.getWorld().getName());
        progressData.put("biome", entity.getLocation().getBlock().getBiome().getKey().getKey());
        
        // Check if it's a boss-like entity
        if (isBossLike(entity)) {
            progressData.put("boss", true);
        }
        
        // Determine weapon type
        if (killer.getInventory().getItemInMainHand().getType().name().contains("BOW") ||
            killer.getInventory().getItemInMainHand().getType().name().contains("CROSSBOW")) {
            progressData.put("weapon_type", "RANGED");
        } else {
            progressData.put("weapon_type", "MELEE");
        }
        
        // Add progress to hunting quests (old system)
        plugin.getQuestManager().addProgress(killer.getUniqueId(), Quest.QuestType.HUNTING, progressData, 1);
        
        // NEW SYSTEM: Track ONLY the actual mob type that was killed
        // The quest matching logic will check if this mob is in the quest's mob_types list
        // Example: Killing CREEPER will ONLY match quests that have "CREEPER" in their mob_types
        
        String mobTarget = entity.getType().name();
        
        // Track mob kill quests (e.g., "Kill 10 Zombies" with mob_types: [ZOMBIE, ZOMBIE_VILLAGER, HUSK])
        // This will ONLY match quests that explicitly list this mob type in their requirements
        plugin.getDailyQuestManager().addProgress(killer.getUniqueId(), com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.HUNTING, mobTarget, 1);
        plugin.getWeeklyQuestManager().addProgress(killer.getUniqueId(), "HUNTING", mobTarget, 1);
        plugin.getGlobalQuestManager().addProgress(killer.getUniqueId(), "HUNTING", mobTarget, 1L);
        
        // Track item drops for collection quests (e.g., "Collect 20 Leather" with items: [LEATHER])
        // This will ONLY match quests that explicitly list this item type in their requirements
        // NO double-counting: mob kill tracking happens above, item tracking happens here
        for (org.bukkit.inventory.ItemStack drop : event.getDrops()) {
            if (drop != null && !drop.getType().isAir()) {
                String itemType = drop.getType().name();
                int amount = drop.getAmount();
                
                // Track item collection - will only match quests with this item in their "items" list
                plugin.getDailyQuestManager().addProgress(killer.getUniqueId(), com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.HUNTING, itemType, amount);
                plugin.getWeeklyQuestManager().addProgress(killer.getUniqueId(), "HUNTING", itemType, amount);
                plugin.getGlobalQuestManager().addProgress(killer.getUniqueId(), "HUNTING", itemType, (long) amount);
            }
        }
        
        // Update player statistics
        plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId())
            .addStatistic("mobs_killed", 1);
        plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId())
            .addStatistic("mobs_killed_" + entity.getType().name().toLowerCase(), 1);
    }
    
    private boolean isBossLike(LivingEntity entity) {
        // Check if entity is boss-like (has custom name, high health, etc.)
        return entity.getCustomName() != null ||
               entity.getMaxHealth() > 100 ||
               entity.getType().name().contains("WITHER") ||
               entity.getType().name().contains("DRAGON") ||
               entity.getType().name().contains("ELDER");
    }
    
    private boolean isHostileMob(LivingEntity entity) {
        // Check if entity is a hostile/aggressive mob
        String type = entity.getType().name();
        return type.equals("ZOMBIE") || type.equals("ZOMBIE_VILLAGER") || type.equals("HUSK") || type.equals("DROWNED") ||
               type.equals("SKELETON") || type.equals("STRAY") || type.equals("WITHER_SKELETON") ||
               type.equals("CREEPER") ||
               type.equals("SPIDER") || type.equals("CAVE_SPIDER") ||
               type.equals("ENDERMAN") ||
               type.equals("WITCH") ||
               type.equals("BLAZE") ||
               type.equals("GHAST") ||
               type.equals("MAGMA_CUBE") ||
               type.equals("SLIME") ||
               type.equals("PHANTOM") ||
               type.equals("VEX") ||
               type.equals("VINDICATOR") ||
               type.equals("PILLAGER") ||
               type.equals("RAVAGER") ||
               type.equals("EVOKER") ||
               type.equals("SILVERFISH") ||
               type.equals("ENDERMITE") ||
               type.equals("GUARDIAN") ||
               type.equals("ELDER_GUARDIAN") ||
               type.equals("SHULKER") ||
               type.equals("HOGLIN") ||
               type.equals("ZOGLIN") ||
               type.equals("PIGLIN_BRUTE") ||
               type.equals("WARDEN") ||
               type.equals("WITHER") ||
               type.equals("ENDER_DRAGON");
    }
    
    private boolean isPassiveMob(LivingEntity entity) {
        // Check if entity is a passive/neutral mob
        String type = entity.getType().name();
        return type.equals("COW") || type.equals("SHEEP") || type.equals("PIG") || type.equals("CHICKEN") ||
               type.equals("RABBIT") || type.equals("HORSE") || type.equals("DONKEY") || type.equals("MULE") ||
               type.equals("LLAMA") || type.equals("WOLF") || type.equals("CAT") || type.equals("OCELOT") ||
               type.equals("PARROT") || type.equals("VILLAGER") || type.equals("TRADER_LLAMA") ||
               type.equals("WANDERING_TRADER") || type.equals("FOX") || type.equals("PANDA") ||
               type.equals("POLAR_BEAR") || type.equals("BEE") || type.equals("STRIDER") ||
               type.equals("AXOLOTL") || type.equals("GLOW_SQUID") || type.equals("SQUID") ||
               type.equals("BAT") || type.equals("MUSHROOM_COW") || type.equals("TURTLE") ||
               type.equals("COD") || type.equals("SALMON") || type.equals("PUFFERFISH") || type.equals("TROPICAL_FISH") ||
               type.equals("DOLPHIN") || type.equals("GOAT") || type.equals("FROG") || type.equals("TADPOLE") ||
               type.equals("ALLAY") || type.equals("CAMEL") || type.equals("SNIFFER") ||
               type.equals("IRON_GOLEM") || type.equals("SNOW_GOLEM");
    }
}
