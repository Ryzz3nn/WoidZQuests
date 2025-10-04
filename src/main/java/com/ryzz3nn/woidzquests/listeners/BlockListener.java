package com.ryzz3nn.woidzquests.listeners;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.Quest;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;

public class BlockListener implements Listener {
    
    private final WoidZQuests plugin;
    
    public BlockListener(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if block was placed by a player (anti-cheese)
        if (isPlayerPlaced(block)) {
            return;
        }
        
        // Create progress data
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("material", block.getType().name());
        progressData.put("world", block.getWorld().getName());
        progressData.put("x", block.getX());
        progressData.put("y", block.getY());
        progressData.put("z", block.getZ());
        progressData.put("biome", block.getBiome().getKey().getKey());
        
        // Determine quest type based on material (old system)
        Quest.QuestType questType = getQuestTypeForMaterial(block.getType());
        if (questType != null) {
            plugin.getQuestManager().addProgress(player.getUniqueId(), questType, progressData, 1);
        }
        
        // Quest progress tracking (new system)
        if (isOre(block.getType()) || isStone(block.getType())) {
            String materialTarget = block.getType().name();
            plugin.getDailyQuestManager().addProgress(player.getUniqueId(), com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.MINING, materialTarget, 1);
            plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "MINING", materialTarget, 1);
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "MINING", materialTarget, 1L);
        }
        
        // Track wood/log breaking for woodcutting quests
        if (isWood(block.getType())) {
            String materialTarget = block.getType().name();
            plugin.getDailyQuestManager().addProgress(player.getUniqueId(), com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.WOODCUTTING, materialTarget, 1);
            plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "WOODCUTTING", materialTarget, 1);
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "WOODCUTTING", materialTarget, 1L);
        }
        
        // Update player statistics
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("blocks_broken", 1);
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("blocks_broken_" + block.getType().name().toLowerCase(), 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Track placed blocks for anti-cheese
        trackPlacedBlock(block, player);
        
        // Create progress data for building quests
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("material", block.getType().name());
        progressData.put("world", block.getWorld().getName());
        progressData.put("x", block.getX());
        progressData.put("y", block.getY());
        progressData.put("z", block.getZ());
        progressData.put("biome", block.getBiome().getKey().getKey());
        
        // Add progress to building quests (old system)
        plugin.getQuestManager().addProgress(player.getUniqueId(), Quest.QuestType.BUILDING, progressData, 1);
        
        // Daily quest progress tracking (new system)
        String materialTarget = block.getType().name();
        
        plugin.getDailyQuestManager().addProgress(player.getUniqueId(), com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.BUILDING, materialTarget, 1);
        plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "BUILDING", materialTarget, 1);
        plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "BUILDING", materialTarget, 1L);
        
        // Also check for wood planks category
        if (isWoodPlanks(block.getType())) {
            plugin.getDailyQuestManager().addProgress(player.getUniqueId(), com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.BUILDING, "WOOD_PLANKS", 1);
            plugin.getWeeklyQuestManager().addProgress(player.getUniqueId(), "BUILDING", "WOOD_PLANKS", 1);
            plugin.getGlobalQuestManager().addProgress(player.getUniqueId(), "BUILDING", "WOOD_PLANKS", 1L);
        }
        
        // Update player statistics
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("blocks_placed", 1);
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .addStatistic("blocks_placed_" + block.getType().name().toLowerCase(), 1);
    }
    
    private Quest.QuestType getQuestTypeForMaterial(Material material) {
        // Determine quest type based on material
        if (isOre(material) || isStone(material)) {
            return Quest.QuestType.MINING;
        } else if (isWood(material)) {
            return Quest.QuestType.WOODCUTTING;
        } else if (isCrop(material)) {
            return Quest.QuestType.FARMING;
        }
        
        return null;
    }
    
    private boolean isOre(Material material) {
        return material.name().contains("_ORE") || 
               material == Material.COAL || 
               material == Material.DIAMOND || 
               material == Material.EMERALD ||
               material == Material.REDSTONE ||
               material == Material.LAPIS_LAZULI;
    }
    
    private boolean isStone(Material material) {
        return material == Material.STONE ||
               material == Material.COBBLESTONE ||
               material == Material.DEEPSLATE ||
               material == Material.COBBLED_DEEPSLATE ||
               material.name().contains("STONE");
    }
    
    private boolean isWood(Material material) {
        return material.name().contains("_LOG") ||
               material.name().contains("_WOOD") ||
               material.name().contains("STRIPPED_");
    }
    
    private boolean isCrop(Material material) {
        return material == Material.WHEAT ||
               material == Material.CARROTS ||
               material == Material.POTATOES ||
               material == Material.BEETROOTS ||
               material == Material.NETHER_WART ||
               material == Material.COCOA ||
               material.name().contains("BERRIES");
    }
    
    private boolean isPlayerPlaced(Block block) {
        // Check if anti-cheese system is enabled
        if (!plugin.getConfigManager().getBoolean("quests.anti-cheese.track-placed-blocks", true)) {
            return false;
        }
        
        // Check database to see if this block was placed by a player
        try (var connection = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                SELECT player_uuid, placed_at FROM placed_blocks 
                WHERE world = ? AND x = ? AND y = ? AND z = ?
            """;
            
            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, block.getWorld().getName());
                statement.setInt(2, block.getX());
                statement.setInt(3, block.getY());
                statement.setInt(4, block.getZ());
                
                try (var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        // Block was placed by a player
                        // Check if it's too old (configurable expiry)
                        long placedAt = resultSet.getLong("placed_at");
                        long trackingDuration = plugin.getConfigManager().getInt("quests.anti-cheese.tracking-duration-hours", 24) * 60 * 60 * 1000L;
                        long age = System.currentTimeMillis() - placedAt;
                        
                        if (age < trackingDuration) {
                            // Block is tracked and still within tracking duration
                            return true;
                        } else {
                            // Block is old enough to be considered "natural" again
                            // Clean up the old entry
                            removeTrackedBlock(block);
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check if block was player-placed: " + e.getMessage());
            // On error, assume block is natural (fail-safe)
            return false;
        }
        
        return false;
    }
    
    private void trackPlacedBlock(Block block, Player player) {
        if (!plugin.getConfigManager().getBoolean("quests.anti-cheese.track-placed-blocks", true)) {
            return;
        }
        
        plugin.getDatabaseManager().executeAsync(connection -> {
            String sql = """
                INSERT OR REPLACE INTO placed_blocks (world, x, y, z, material, player_uuid, placed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, block.getWorld().getName());
                statement.setInt(2, block.getX());
                statement.setInt(3, block.getY());
                statement.setInt(4, block.getZ());
                statement.setString(5, block.getType().name());
                statement.setString(6, player.getUniqueId().toString());
                statement.setLong(7, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to track placed block: " + e.getMessage());
            }
        });
    }
    
    private void removeTrackedBlock(Block block) {
        plugin.getDatabaseManager().executeAsync(connection -> {
            String sql = """
                DELETE FROM placed_blocks 
                WHERE world = ? AND x = ? AND y = ? AND z = ?
            """;
            
            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, block.getWorld().getName());
                statement.setInt(2, block.getX());
                statement.setInt(3, block.getY());
                statement.setInt(4, block.getZ());
                statement.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to remove tracked block: " + e.getMessage());
            }
        });
    }
    
    private boolean isWoodPlanks(Material material) {
        return material == Material.OAK_PLANKS || 
               material == Material.BIRCH_PLANKS || 
               material == Material.SPRUCE_PLANKS || 
               material == Material.JUNGLE_PLANKS || 
               material == Material.ACACIA_PLANKS || 
               material == Material.DARK_OAK_PLANKS || 
               material == Material.MANGROVE_PLANKS || 
               material == Material.CHERRY_PLANKS || 
               material == Material.BAMBOO_PLANKS || 
               material == Material.CRIMSON_PLANKS || 
               material == Material.WARPED_PLANKS;
    }
}
