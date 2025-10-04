package com.ryzz3nn.woidzquests.managers;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.GlobalQuest;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class GlobalQuestManager {

    private final WoidZQuests plugin;
    private final List<GlobalQuest> activeGlobalQuests = new ArrayList<>();
    private final Map<String, BukkitTask> refillTasks = new HashMap<>();
    private final List<GlobalQuestTemplate> questTemplates = new ArrayList<>();
    private final Gson gson = new Gson();

    public GlobalQuestManager(WoidZQuests plugin) {
        this.plugin = plugin;
        initializeQuestTemplates();
        loadGlobalQuests();
        scheduleProgressAnnouncements();
    }

    private void initializeQuestTemplates() {
        // Large-scale server goals
        questTemplates.add(new GlobalQuestTemplate("MINING", "STONE", 
            "<gray>Server Goal: Mine <white>{amount} Stone</white> blocks together!", 
            Material.STONE, 100000L, 500000L, 10000.0, 2000, 200));
        
        questTemplates.add(new GlobalQuestTemplate("MINING", "DIAMOND_ORE", 
            "<gray>Server Goal: Mine <aqua>{amount} Diamond Ore</aqua> blocks together!", 
            Material.DIAMOND_ORE, 5000L, 15000L, 15000.0, 3000, 300));
        
        questTemplates.add(new GlobalQuestTemplate("FISHING", "COD", 
            "<gray>Server Goal: Catch <blue>{amount} Fish</blue> together!", 
            Material.COD, 50000L, 150000L, 8000.0, 1500, 150));
        
        questTemplates.add(new GlobalQuestTemplate("HUNTING", "ZOMBIE", 
            "<gray>Server Goal: Defeat <green>{amount} Monsters</green> together!", 
            Material.ROTTEN_FLESH, 75000L, 200000L, 12000.0, 2500, 250));
        
        questTemplates.add(new GlobalQuestTemplate("BUILDING", "WOOD_PLANKS", 
            "<gray>Server Goal: Place <yellow>{amount} Wood Planks</yellow> together!", 
            Material.OAK_PLANKS, 200000L, 600000L, 7000.0, 1200, 120));
        
        questTemplates.add(new GlobalQuestTemplate("FARMING", "WHEAT", 
            "<gray>Server Goal: Harvest <yellow>{amount} Crops</yellow> together!", 
            Material.WHEAT, 150000L, 400000L, 6000.0, 1000, 100));
    }

    private void loadGlobalQuests() {
        try (Connection connection = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM global_quests WHERE completed = FALSE";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                
                while (resultSet.next()) {
                    GlobalQuest quest = deserializeGlobalQuest(resultSet);
                    if (quest != null) {
                        activeGlobalQuests.add(quest);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load global quests from database: " + e.getMessage());
        }
        
        // If no quests loaded from database, generate initial quests
        if (activeGlobalQuests.isEmpty()) {
            generateInitialGlobalQuests();
        }
        
        plugin.getLogger().info("Loaded " + activeGlobalQuests.size() + " active global quests");
    }
    
    private void generateInitialGlobalQuests() {
        // Generate 3 initial global quests
        List<GlobalQuestTemplate> availableTemplates = new ArrayList<>(questTemplates);
        Collections.shuffle(availableTemplates);
        
        for (int i = 0; i < Math.min(3, availableTemplates.size()); i++) {
            GlobalQuestTemplate template = availableTemplates.get(i);
            GlobalQuest quest = createQuestFromTemplate(template);
            activeGlobalQuests.add(quest);
            saveGlobalQuest(quest); // Save to database
        }
    }

    public List<GlobalQuest> getActiveGlobalQuests() {
        return new ArrayList<>(activeGlobalQuests);
    }

    public void addProgress(UUID playerId, String questType, String target, long amount) {
        for (GlobalQuest quest : activeGlobalQuests) {
            if (quest.getType().equals(questType) && 
                (quest.getTarget().equals(target) || isMatchingCategory(quest.getTarget(), target)) &&
                !quest.isCompleted()) {
                
                long oldProgress = quest.getCurrentProgress();
                quest.addProgress(playerId, amount);
                
                // Save progress to database
                saveGlobalQuest(quest);
                
                // Announce major milestones (every 25%)
                if (shouldAnnounceMilestone(oldProgress, quest.getCurrentProgress(), quest.getTargetAmount())) {
                    announceMilestone(quest);
                }
                
                // Announce completion and schedule refill
                if (quest.isCompleted() && oldProgress < quest.getTargetAmount()) {
                    announceCompletion(quest);
                    scheduleQuestRefill(quest);
                }
                
                break;
            }
        }
    }

    private boolean shouldAnnounceMilestone(long oldProgress, long newProgress, long target) {
        int oldPercentage = (int) ((oldProgress * 100) / target);
        int newPercentage = (int) ((newProgress * 100) / target);
        return (newPercentage / 25) > (oldPercentage / 25); // Every 25%
    }

    private void announceMilestone(GlobalQuest quest) {
        int percentage = quest.getProgressPercentage();
        Component message = plugin.parseMessage(
            "<gray>[<gold><bold>Global Quest</bold></gold><gray>]<reset> " +
            quest.getProgressColor() + quest.getName() + "<reset> is now " +
            quest.getProgressColor() + percentage + "%<reset> complete! " +
            "<gray>(" + quest.getFormattedProgress() + "/" + quest.getFormattedTarget() + ")"
        );
        
        Bukkit.broadcast(message);
    }

    private void announceCompletion(GlobalQuest quest) {
        Component message = plugin.parseMessage(
            "<gray>[<gold><bold>Global Quest Complete!</bold></gold><gray>]<reset> " +
            "<gold>" + quest.getName() + "<reset> has been completed by the server! " +
            "<yellow>Use /quests to claim your reward!"
        );
        
        Bukkit.broadcast(message);
    }

    private boolean isMatchingCategory(String questTarget, String actualTarget) {
        // Handle wood planks category
        if ("WOOD_PLANKS".equals(questTarget)) {
            return actualTarget.endsWith("_PLANKS");
        }
        return false;
    }

    public boolean claimReward(UUID playerId, String questId) {
        for (GlobalQuest quest : activeGlobalQuests) {
            if (quest.getId().equals(questId) && quest.isCompleted() && !quest.hasPlayerClaimed(playerId)) {
                
                // Check if player contributed to the quest
                if (quest.getPlayerContribution(playerId) == 0) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> You must contribute to this global quest to claim rewards!"));
                    }
                    return false;
                }
                
                quest.setPlayerClaimed(playerId, true);
                
                // Save claimed status to database
                saveGlobalQuest(quest);
                
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    GlobalQuest.GlobalReward reward = quest.getReward();
                    
                    // Give money reward
                    if (reward.getMoney() > 0 && plugin.getVaultIntegration() != null) {
                        plugin.getVaultIntegration().deposit(player, reward.getMoney());
                        player.sendMessage(plugin.parseMessage("<gray>[<green><bold>$</bold></green><gray>]<reset> Received <green>$" + reward.getMoney() + "<reset>!"));
                    }
                    
                    // Give XP reward
                    if (reward.getExperience() > 0) {
                        player.giveExp(reward.getExperience());
                        player.sendMessage(plugin.parseMessage("<gray>[<aqua><bold>✦</bold></aqua><gray>]<reset> Received <aqua>" + reward.getExperience() + " XP<reset>!"));
                    }
                    
                    // Quest Points reward (standardized: 10 QP for global quests)
                    String qpCommand = "qp give " + player.getName() + " 10";
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), qpCommand);
                    player.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>★</bold></light_purple><gray>]<reset> Received <light_purple>10 Quest Points<reset>!"));
                    
                    // Command-based rewards for maximum flexibility
                    if (reward.getCommands() != null && !reward.getCommands().isEmpty()) {
                        for (String command : reward.getCommands()) {
                            String processedCommand = command.replace("{player}", player.getName())
                                                           .replace("{amount}", String.valueOf(reward.getQuestPoints()));
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                        }
                    }
                    
                    long contribution = quest.getPlayerContribution(playerId);
                    player.sendMessage(plugin.parseMessage("<gray>[<green><bold>✓</bold></green><gray>]<reset> Claimed reward for: <yellow>" + quest.getName() + "<reset>!"));
                    player.sendMessage(plugin.parseMessage("<gray>Your contribution: <gold>" + formatLargeNumber(contribution) + "<reset>"));
                }
                
                return true;
            }
        }
        
        return false;
    }

    private void scheduleProgressAnnouncements() {
        // Announce global quest progress every 30 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                for (GlobalQuest quest : activeGlobalQuests) {
                    if (!quest.isCompleted() && quest.getCurrentProgress() > 0) {
                        Component message = plugin.parseMessage(
                            "<gray>[<gold><bold>Global Quest</bold></gold><gray>]<reset> " +
                            quest.getProgressColor() + quest.getName() + "<reset> - " +
                            quest.getProgressColor() + quest.getProgressPercentage() + "%<reset> complete " +
                            "<gray>(" + quest.getFormattedProgress() + "/" + quest.getFormattedTarget() + ")"
                        );
                        
                        Bukkit.broadcast(message);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 60L * 30L, 20L * 60L * 30L); // Every 30 minutes
    }

    private GlobalQuest createQuestFromTemplate(GlobalQuestTemplate template) {
        Random random = new Random();
        long amount = template.minAmount + (long) (random.nextDouble() * (template.maxAmount - template.minAmount));
        
        String questName = template.description.replace("{amount}", formatLargeNumber(amount));
        String questId = "global_" + template.type.toLowerCase() + "_" + template.target.toLowerCase() + "_" + System.currentTimeMillis();
        
        GlobalQuest.GlobalReward reward = new GlobalQuest.GlobalReward(
            template.baseMoney, template.baseExperience, template.baseQuestPoints);
        
        return new GlobalQuest(questId, questName, questName, template.type, template.target, 
                              amount, template.displayMaterial, reward);
    }

    private void scheduleQuestRefill(GlobalQuest completedQuest) {
        // Cancel any existing refill task for this quest
        BukkitTask existingTask = refillTasks.get(completedQuest.getId());
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Schedule new quest to replace this one in 30 minutes (36000 ticks)
        BukkitTask refillTask = new BukkitRunnable() {
            @Override
            public void run() {
                refillCompletedQuest(completedQuest);
                refillTasks.remove(completedQuest.getId());
            }
        }.runTaskLater(plugin, 36000L); // 30 minutes = 30 * 60 * 20 ticks
        
        refillTasks.put(completedQuest.getId(), refillTask);
        
        // Announce the refill schedule
        Component announcement = plugin.parseMessage(
            "<gray>[<gold><bold>Global Quest</bold></gold><gray>]<reset> " +
            "<yellow>A new global quest will be available in <gold>30 minutes<reset>!"
        );
        Bukkit.broadcast(announcement);
    }
    
    private void refillCompletedQuest(GlobalQuest completedQuest) {
        // Mark quest as completed in database and remove from active list
        markQuestCompleted(completedQuest);
        activeGlobalQuests.remove(completedQuest);
        
        // Generate a new quest to replace it
        List<GlobalQuestTemplate> availableTemplates = new ArrayList<>(questTemplates);
        Collections.shuffle(availableTemplates);
        
        // Find a template that's not currently active
        GlobalQuestTemplate selectedTemplate = null;
        for (GlobalQuestTemplate template : availableTemplates) {
            boolean isActive = activeGlobalQuests.stream()
                .anyMatch(quest -> quest.getType().equals(template.type) && quest.getTarget().equals(template.target));
            
            if (!isActive) {
                selectedTemplate = template;
                break;
            }
        }
        
        // If all templates are active, just pick the first one (shouldn't happen with enough templates)
        if (selectedTemplate == null && !availableTemplates.isEmpty()) {
            selectedTemplate = availableTemplates.get(0);
        }
        
        if (selectedTemplate != null) {
            GlobalQuest newQuest = createQuestFromTemplate(selectedTemplate);
            activeGlobalQuests.add(newQuest);
            
            // Save new quest to database
            saveGlobalQuest(newQuest);
            
            // Announce the new quest
            Component announcement = plugin.parseMessage(
                "<gray>[<gold><bold>New Global Quest!</bold></gold><gray>]<reset> " +
                "<yellow>" + newQuest.getName() + "<reset> is now available! " +
                "<gray>Use /quests to participate!"
            );
            Bukkit.broadcast(announcement);
        }
    }
    
    private String formatLargeNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
    
    private void markQuestCompleted(GlobalQuest quest) {
        plugin.getDatabaseManager().executeAsync(connection -> {
            String sql = "UPDATE global_quests SET completed = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, quest.getId());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to mark global quest as completed " + quest.getId() + ": " + e.getMessage());
            }
        });
    }
    
    private void saveGlobalQuest(GlobalQuest quest) {
        plugin.getDatabaseManager().executeAsync(connection -> {
            String sql = """
                INSERT OR REPLACE INTO global_quests 
                (id, name, description, type, target, target_amount, current_progress, completed,
                 display_material, reward_data, player_contributions, player_claimed, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, quest.getId());
                statement.setString(2, quest.getName());
                statement.setString(3, quest.getDescription());
                statement.setString(4, quest.getType());
                statement.setString(5, quest.getTarget());
                statement.setLong(6, quest.getTargetAmount());
                statement.setLong(7, quest.getCurrentProgress());
                statement.setBoolean(8, quest.isCompleted());
                statement.setString(9, quest.getDisplayMaterial().name());
                statement.setString(10, gson.toJson(quest.getReward()));
                statement.setString(11, gson.toJson(quest.getPlayerContributions()));
                statement.setString(12, gson.toJson(quest.getPlayerClaimed()));
                
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save global quest " + quest.getId() + ": " + e.getMessage());
            }
        });
    }
    
    private GlobalQuest deserializeGlobalQuest(ResultSet resultSet) throws SQLException {
        try {
            String id = resultSet.getString("id");
            String name = resultSet.getString("name");
            String description = resultSet.getString("description");
            String type = resultSet.getString("type");
            String target = resultSet.getString("target");
            long targetAmount = resultSet.getLong("target_amount");
            long currentProgress = resultSet.getLong("current_progress");
            boolean completed = resultSet.getBoolean("completed");
            Material displayMaterial = Material.valueOf(resultSet.getString("display_material"));
            
            // Deserialize reward
            String rewardJson = resultSet.getString("reward_data");
            GlobalQuest.GlobalReward reward = gson.fromJson(rewardJson, GlobalQuest.GlobalReward.class);
            
            // Create quest
            GlobalQuest quest = new GlobalQuest(id, name, description, type, target, targetAmount, displayMaterial, reward);
            quest.setCurrentProgress(currentProgress);
            quest.setCompleted(completed);
            
            // Deserialize player contributions
            String contributionsJson = resultSet.getString("player_contributions");
            if (contributionsJson != null && !contributionsJson.equals("{}")) {
                Map<String, Long> contributions = gson.fromJson(contributionsJson, new TypeToken<Map<String, Long>>(){}.getType());
                for (Map.Entry<String, Long> entry : contributions.entrySet()) {
                    quest.getPlayerContributions().put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
            
            // Deserialize player claimed status
            String claimedJson = resultSet.getString("player_claimed");
            if (claimedJson != null && !claimedJson.equals("{}")) {
                Map<String, Boolean> claimed = gson.fromJson(claimedJson, new TypeToken<Map<String, Boolean>>(){}.getType());
                for (Map.Entry<String, Boolean> entry : claimed.entrySet()) {
                    quest.getPlayerClaimed().put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
            
            return quest;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize global quest: " + e.getMessage());
            return null;
        }
    }
    
    public void cleanup() {
        // Save all active global quests before shutdown
        for (GlobalQuest quest : activeGlobalQuests) {
            saveGlobalQuest(quest);
        }
        
        // Cancel all pending refill tasks
        for (BukkitTask task : refillTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        refillTasks.clear();
    }

    private static class GlobalQuestTemplate {
        String type;
        String target;
        String description;
        Material displayMaterial;
        long minAmount;
        long maxAmount;
        double baseMoney;
        int baseExperience;
        int baseQuestPoints;

        GlobalQuestTemplate(String type, String target, String description, Material displayMaterial,
                           long minAmount, long maxAmount, double baseMoney, int baseExperience, int baseQuestPoints) {
            this.type = type;
            this.target = target;
            this.description = description;
            this.displayMaterial = displayMaterial;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.baseMoney = baseMoney;
            this.baseExperience = baseExperience;
            this.baseQuestPoints = baseQuestPoints;
        }
    }
}
