package com.ryzz3nn.woidzquests.managers;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.PlayerData;
import com.ryzz3nn.woidzquests.models.WeeklyQuest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class WeeklyQuestManager {

    private final WoidZQuests plugin;
    private final Map<UUID, List<WeeklyQuest>> playerWeeklyQuests = new HashMap<>();
    private final List<WeeklyQuestTemplate> questTemplates = new ArrayList<>();

    public WeeklyQuestManager(WoidZQuests plugin) {
        this.plugin = plugin;
        initializeQuestTemplates();
        scheduleWeeklyReset();
    }

    private void initializeQuestTemplates() {
        // Mining quests
        questTemplates.add(new WeeklyQuestTemplate("MINING", "DIAMOND_ORE", 
            "<gray>Mine <aqua>{amount} Diamond Ore</aqua> blocks", 
            Material.DIAMOND_ORE, 50, 150, 2500.0, 1000, 100));
        
        questTemplates.add(new WeeklyQuestTemplate("MINING", "IRON_ORE", 
            "<gray>Mine <white>{amount} Iron Ore</white> blocks", 
            Material.IRON_ORE, 200, 500, 1500.0, 800, 75));
        
        // Fishing quests
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "COD", 
            "<gray>Catch <blue>{amount} Cod</blue> fish", 
            Material.COD, 100, 300, 1200.0, 600, 60));
            
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "ANY_FISH", 
            "<gray>Catch <aqua>{amount} Fish</aqua> of any type", 
            Material.FISHING_ROD, 200, 500, 2000.0, 1000, 100));
            
        // WoidZFishing Integration Weekly Quests
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "RARE_FISH", 
            "<gray>Catch <light_purple>{amount} Rare+ Fish</light_purple>", 
            Material.TROPICAL_FISH, 15, 40, 3000.0, 1500, 150));
            
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "ULTRA_RARE_FISH", 
            "<gray>Catch <gold>{amount} Ultra Rare Fish</gold>", 
            Material.ENCHANTED_GOLDEN_APPLE, 1, 3, 5000.0, 2500, 250));
            
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "VALUABLE_FISH", 
            "<gray>Catch <yellow>{amount} Valuable Fish</yellow> ($100+ each)", 
            Material.GOLD_INGOT, 10, 25, 2500.0, 1200, 120));
            
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "LARGE_FISH", 
            "<gray>Catch <green>{amount} Large Fish</green> (50cm+)", 
            Material.PUFFERFISH, 20, 50, 2200.0, 1100, 110));
            
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "TIER_5_FISH", 
            "<gray>Catch <red>{amount} Tier 5+ Fish</red>", 
            Material.TROPICAL_FISH_BUCKET, 5, 15, 4000.0, 2000, 200));
            
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "BASS", 
            "<gray>Catch <blue>{amount} Bass</blue>", 
            Material.COD, 30, 80, 1800.0, 900, 90));
            
        questTemplates.add(new WeeklyQuestTemplate("FISHING", "SALMON", 
            "<gray>Catch <red>{amount} Salmon</red>", 
            Material.SALMON, 25, 70, 1600.0, 800, 80));
        
        // Combat quests
        questTemplates.add(new WeeklyQuestTemplate("HUNTING", "ZOMBIE", 
            "<gray>Defeat <green>{amount} Zombies</green>", 
            Material.ROTTEN_FLESH, 150, 400, 1800.0, 900, 80));
        
        questTemplates.add(new WeeklyQuestTemplate("HUNTING", "SKELETON", 
            "<gray>Defeat <white>{amount} Skeletons</white>", 
            Material.BONE, 120, 350, 2000.0, 1000, 85));
        
        // Farming quests
        questTemplates.add(new WeeklyQuestTemplate("FARMING", "WHEAT", 
            "<gray>Harvest <yellow>{amount} Wheat</yellow> crops", 
            Material.WHEAT, 300, 800, 1000.0, 500, 50));
        
        // Building quests
        questTemplates.add(new WeeklyQuestTemplate("BUILDING", "STONE_BRICKS", 
            "<gray>Place <gray>{amount} Stone Bricks</gray>", 
            Material.STONE_BRICKS, 500, 1200, 1500.0, 750, 70));
    }

    public List<WeeklyQuest> getPlayerWeeklyQuests(UUID playerId) {
        return playerWeeklyQuests.computeIfAbsent(playerId, k -> generateNewWeeklyQuests());
    }

    public void addProgress(UUID playerId, String questType, String target, int amount) {
        List<WeeklyQuest> quests = getPlayerWeeklyQuests(playerId);
        
        for (WeeklyQuest quest : quests) {
            if (quest.getType().equals(questType) && 
                (quest.getTarget().equals(target) || isMatchingCategory(quest.getTarget(), target)) &&
                !quest.isCompleted()) {
                
                int oldProgress = quest.getCurrentProgress();
                quest.addProgress(amount);
                
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    // Show progress update with throttling (every 10%)
                    if (shouldShowProgressUpdate(oldProgress, quest.getCurrentProgress(), quest.getTargetAmount())) {
                        plugin.getBossBarManager().showQuestProgress(player, convertToDisplayQuest(quest));
                    }
                    
                    // Show completion notification
                    if (quest.isCompleted() && oldProgress < quest.getTargetAmount()) {
                        plugin.getBossBarManager().showQuestCompleted(player, convertToDisplayQuest(quest));
                    }
                }
                break;
            }
        }
    }

    private boolean shouldShowProgressUpdate(int oldProgress, int newProgress, int target) {
        int oldPercentage = (oldProgress * 100) / target;
        int newPercentage = (newProgress * 100) / target;
        return (newPercentage / 10) > (oldPercentage / 10); // Every 10%
    }

    // Helper method to convert WeeklyQuest to a format compatible with BossBarManager
    private com.ryzz3nn.woidzquests.models.DailyQuest convertToDisplayQuest(WeeklyQuest weeklyQuest) {
        com.ryzz3nn.woidzquests.models.DailyQuest.DailyReward reward = 
            new com.ryzz3nn.woidzquests.models.DailyQuest.DailyReward();
        reward.setMoney((int) weeklyQuest.getReward().getMoney());
        reward.setExperience(weeklyQuest.getReward().getExperience());
        
        // Convert string type to enum
        com.ryzz3nn.woidzquests.models.DailyQuest.QuestType questType = 
            com.ryzz3nn.woidzquests.models.DailyQuest.QuestType.valueOf(weeklyQuest.getType());
        
        com.ryzz3nn.woidzquests.models.DailyQuest displayQuest = 
            new com.ryzz3nn.woidzquests.models.DailyQuest(
                weeklyQuest.getId(),
                weeklyQuest.getName(),
                weeklyQuest.getDescription(),
                questType,
                weeklyQuest.getDisplayMaterial(),
                weeklyQuest.getTarget(),
                weeklyQuest.getTargetAmount(),
                reward
            );
        
        displayQuest.setCurrentProgress(weeklyQuest.getCurrentProgress());
        displayQuest.setCompleted(weeklyQuest.isCompleted());
        displayQuest.setClaimed(weeklyQuest.isClaimed());
        
        return displayQuest;
    }

    private boolean isMatchingCategory(String questTarget, String actualTarget) {
        // Handle wood planks category
        if ("WOOD_PLANKS".equals(questTarget)) {
            return actualTarget.endsWith("_PLANKS");
        }
        return false;
    }

    public boolean claimReward(UUID playerId, String questId) {
        List<WeeklyQuest> quests = getPlayerWeeklyQuests(playerId);
        
        for (WeeklyQuest quest : quests) {
            if (quest.getId().equals(questId) && quest.isCompleted() && !quest.isClaimed()) {
                quest.setClaimed(true);
                
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    WeeklyQuest.WeeklyReward reward = quest.getReward();
                    
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
                    
                    // Quest Points reward (standardized: 5 QP for weekly quests)
                    String qpCommand = "qp give " + player.getName() + " 5";
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), qpCommand);
                    player.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>★</bold></light_purple><gray>]<reset> Received <light_purple>5 Quest Points<reset>!"));
                    
                    // Command-based rewards for maximum flexibility
                    if (reward.getCommands() != null && !reward.getCommands().isEmpty()) {
                        for (String command : reward.getCommands()) {
                            String processedCommand = command.replace("{player}", player.getName())
                                                           .replace("{amount}", String.valueOf(reward.getQuestPoints()));
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                        }
                    }
                    
                    player.sendMessage(plugin.parseMessage("<gray>[<green><bold>✓</bold></green><gray>]<reset> Claimed reward for: <yellow>" + quest.getName() + "<reset>!"));
                }
                
                return true;
            }
        }
        
        return false;
    }

    private void scheduleWeeklyReset() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextMonday = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(0).withMinute(0).withSecond(0);
                
                // Check if it's time for weekly reset (Monday 00:00)
                if (now.getDayOfWeek() == DayOfWeek.MONDAY && now.getHour() == 0 && now.getMinute() == 0) {
                    resetAllWeeklyQuests();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L); // Check every minute
    }

    private void resetAllWeeklyQuests() {
        for (UUID playerId : playerWeeklyQuests.keySet()) {
            generateNewWeeklyQuests(playerId);
            
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(plugin.parseMessage("<gray>[<yellow><bold>Weekly Quests</bold></yellow><gray>]<reset> New weekly quests available! Use <yellow>/quests<reset> to view them."));
            }
        }
    }

    public void generateNewWeeklyQuestsForPlayer(UUID playerId) {
        generateNewWeeklyQuests(playerId);
    }
    
    private void generateNewWeeklyQuests(UUID playerId) {
        List<WeeklyQuest> newQuests = generateNewWeeklyQuests();
        playerWeeklyQuests.put(playerId, newQuests);
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerId);
        // TODO: Save weekly quests to database
    }

    private List<WeeklyQuest> generateNewWeeklyQuests() {
        List<WeeklyQuest> quests = new ArrayList<>();
        List<WeeklyQuestTemplate> availableTemplates = new ArrayList<>(questTemplates);
        Collections.shuffle(availableTemplates);
        
        // Generate 3 random weekly quests
        for (int i = 0; i < Math.min(3, availableTemplates.size()); i++) {
            WeeklyQuestTemplate template = availableTemplates.get(i);
            quests.add(createQuestFromTemplate(template));
        }
        
        return quests;
    }

    private WeeklyQuest createQuestFromTemplate(WeeklyQuestTemplate template) {
        Random random = new Random();
        int amount = template.minAmount + random.nextInt(template.maxAmount - template.minAmount + 1);
        
        String questName = template.description.replace("{amount}", String.valueOf(amount));
        String questId = "weekly_" + template.type.toLowerCase() + "_" + template.target.toLowerCase() + "_" + System.currentTimeMillis();
        
        WeeklyQuest.WeeklyReward reward = new WeeklyQuest.WeeklyReward(
            template.baseMoney, template.baseExperience, template.baseQuestPoints);
        
        return new WeeklyQuest(questId, questName, questName, template.type, template.target, 
                              amount, template.displayMaterial, reward);
    }

    private static class WeeklyQuestTemplate {
        String type;
        String target;
        String description;
        Material displayMaterial;
        int minAmount;
        int maxAmount;
        double baseMoney;
        int baseExperience;
        int baseQuestPoints;

        WeeklyQuestTemplate(String type, String target, String description, Material displayMaterial,
                           int minAmount, int maxAmount, double baseMoney, int baseExperience, int baseQuestPoints) {
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
