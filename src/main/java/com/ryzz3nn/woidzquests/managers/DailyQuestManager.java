package com.ryzz3nn.woidzquests.managers;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.DailyQuest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class DailyQuestManager {
    
    private final WoidZQuests plugin;
    private final Map<UUID, List<DailyQuest>> playerDailyQuests;
    private final List<DailyQuestTemplate> questTemplates;
    private FileConfiguration dailyQuestsConfig;
    
    public DailyQuestManager(WoidZQuests plugin) {
        this.plugin = plugin;
        this.playerDailyQuests = new HashMap<>();
        this.questTemplates = new ArrayList<>();
        
        loadDailyQuestsConfig();
        initializeQuestTemplates();
        startDailyResetScheduler();
    }
    
    private void loadDailyQuestsConfig() {
        try {
            File dailyQuestsFile = new File(plugin.getDataFolder(), "DailyQuests.yml");
            if (!dailyQuestsFile.exists()) {
                plugin.saveResource("DailyQuests.yml", false);
            }
            dailyQuestsConfig = YamlConfiguration.loadConfiguration(dailyQuestsFile);
            plugin.getLogger().info("Loaded DailyQuests.yml configuration");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load DailyQuests.yml", e);
        }
    }
    
    private void initializeQuestTemplates() {
        questTemplates.clear();
        
        if (dailyQuestsConfig == null) {
            plugin.getLogger().warning("DailyQuests.yml not loaded, no daily quests available!");
            return;
        }
        
        Set<String> questKeys = dailyQuestsConfig.getKeys(false);
        plugin.getLogger().info("Loading " + questKeys.size() + " daily quest templates...");
        
        for (String questId : questKeys) {
            ConfigurationSection questSection = dailyQuestsConfig.getConfigurationSection(questId);
            if (questSection == null) continue;
            
            try {
                // Check if quest is enabled
                if (!questSection.getBoolean("enabled", true)) {
                    continue;
                }
                
                // Load basic quest data
                String name = questSection.getString("name", "Unknown Quest");
                String description = questSection.getString("description", "No description");
                String typeStr = questSection.getString("type", "MINING");
                int targetMin = questSection.getInt("target_min", 10);
                int targetMax = questSection.getInt("target_max", 50);
                int weight = questSection.getInt("weight", 10);
                
                // Parse quest type
                DailyQuest.QuestType questType = parseQuestType(typeStr);
                
                // Load requirements
                Map<String, Object> requirements = new HashMap<>();
                ConfigurationSection reqSection = questSection.getConfigurationSection("requirements");
                if (reqSection != null) {
                    if (reqSection.contains("materials")) {
                        requirements.put("materials", reqSection.getStringList("materials"));
                    }
                    if (reqSection.contains("worlds")) {
                        requirements.put("worlds", reqSection.getStringList("worlds"));
                    }
                    if (reqSection.contains("biomes")) {
                        requirements.put("biomes", reqSection.getStringList("biomes"));
                    }
                    if (reqSection.contains("y_min")) {
                        requirements.put("y_min", reqSection.getInt("y_min"));
                    }
                    if (reqSection.contains("y_max")) {
                        requirements.put("y_max", reqSection.getInt("y_max"));
                    }
                }
                
                // Load rewards
                ConfigurationSection rewardSection = questSection.getConfigurationSection("rewards");
                int moneyMin = 0, moneyMax = 0, expMin = 0, expMax = 0;
                List<String> itemRewards = new ArrayList<>();
                List<String> commandRewards = new ArrayList<>();
                
                if (rewardSection != null) {
                    if (rewardSection.contains("money")) {
                        List<Integer> money = rewardSection.getIntegerList("money");
                        if (money.size() >= 2) {
                            moneyMin = money.get(0);
                            moneyMax = money.get(1);
                        } else if (money.size() == 1) {
                            moneyMin = moneyMax = money.get(0);
                        }
                    }
                    
                    if (rewardSection.contains("experience")) {
                        List<Integer> exp = rewardSection.getIntegerList("experience");
                        if (exp.size() >= 2) {
                            expMin = exp.get(0);
                            expMax = exp.get(1);
                        } else if (exp.size() == 1) {
                            expMin = expMax = exp.get(0);
                        }
                    }
                    
                    if (rewardSection.contains("items")) {
                        itemRewards = rewardSection.getStringList("items");
                    }
                    
                    if (rewardSection.contains("commands")) {
                        commandRewards = rewardSection.getStringList("commands");
                    }
                }
                
                // Determine target for matching based on quest type
                String materialTarget = "ANY";
                
                // For hunting quests, check mob_types or items
                if (questType == DailyQuest.QuestType.HUNTING) {
                    if (requirements.containsKey("mob_types")) {
                        List<String> mobTypes = (List<String>) requirements.get("mob_types");
                        if (!mobTypes.isEmpty()) {
                            materialTarget = mobTypes.get(0); // Use first mob type as primary target
                        }
                    } else if (requirements.containsKey("items")) {
                        List<String> items = (List<String>) requirements.get("items");
                        if (!items.isEmpty()) {
                            materialTarget = items.get(0); // Use first item as primary target
                        }
                    }
                }
                // For mining/woodcutting/farming/building, check materials
                else if (requirements.containsKey("materials")) {
                    List<String> materials = (List<String>) requirements.get("materials");
                    if (!materials.isEmpty()) {
                        materialTarget = materials.get(0); // Use first material as primary target
                    }
                }
                
                // Determine display material
                Material displayMaterial = Material.STONE;
                if (requirements.containsKey("materials")) {
                    List<String> materials = (List<String>) requirements.get("materials");
                    if (!materials.isEmpty()) {
                        try {
                            String materialName = materials.get(0);
                            // Convert crop blocks to item equivalents
                            materialName = convertCropBlockToItem(materialName);
                            displayMaterial = Material.valueOf(materialName);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid material for display: " + materials.get(0) + ", using STONE");
                        }
                    }
                }
                
                DailyQuestTemplate template = new DailyQuestTemplate(
                    questId, name, description, questType, displayMaterial, materialTarget,
                    targetMin, targetMax, moneyMin, moneyMax, expMin, expMax, 
                    itemRewards, commandRewards, weight, requirements
                );
                
                questTemplates.add(template);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load daily quest: " + questId, e);
            }
        }
        
        plugin.getLogger().info("Loaded " + questTemplates.size() + " daily quest templates!");
    }
    
    public List<DailyQuest> getPlayerDailyQuests(UUID playerId) {
        List<DailyQuest> quests = playerDailyQuests.computeIfAbsent(playerId, k -> {
            plugin.getLogger().info("Generating new daily quests for player: " + playerId);
            List<DailyQuest> generated = generateNewDailyQuests();
            plugin.getLogger().info("Generated " + generated.size() + " daily quests for player");
            return generated;
        });
        return quests;
    }
    
    public void generateNewDailyQuestsForPlayer(UUID playerId) {
        playerDailyQuests.put(playerId, generateNewDailyQuests());
        
        // TODO: Save daily quests to database
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.parseMessage("<gray>[<yellow><bold>Daily Quests</bold></yellow><gray>]<reset> New daily quests available! Use <yellow>/quests<reset> to view them."));
        }
    }
    
    private List<DailyQuest> generateNewDailyQuests() {
        List<DailyQuest> quests = new ArrayList<>();
        List<DailyQuestTemplate> availableTemplates = new ArrayList<>(questTemplates);
        
        if (availableTemplates.isEmpty()) {
            plugin.getLogger().warning("No quest templates available for daily quest generation!");
            return quests;
        }
        
        // Generate 3-5 random quests using weighted selection
        int questCount = ThreadLocalRandom.current().nextInt(3, 6); // 3-5 quests
        for (int i = 0; i < questCount && !availableTemplates.isEmpty(); i++) {
            DailyQuestTemplate template = selectWeightedRandom(availableTemplates);
            if (template != null) {
                DailyQuest quest = createQuestFromTemplate(template);
                quests.add(quest);
                availableTemplates.remove(template); // Don't select the same quest twice
            }
        }
        
        return quests;
    }
    
    private DailyQuestTemplate selectWeightedRandom(List<DailyQuestTemplate> templates) {
        if (templates.isEmpty()) return null;
        
        // Calculate total weight
        int totalWeight = templates.stream().mapToInt(t -> t.weight).sum();
        
        if (totalWeight <= 0) {
            // If no weights, use simple random
            return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
        }
        
        // Select based on weight
        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;
        
        for (DailyQuestTemplate template : templates) {
            currentWeight += template.weight;
            if (randomValue < currentWeight) {
                return template;
            }
        }
        
        // Fallback (should not happen)
        return templates.get(0);
    }
    
    private DailyQuest createQuestFromTemplate(DailyQuestTemplate template) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int targetAmount = random.nextInt(template.minAmount, template.maxAmount + 1);
        
        String description = template.description.replace("{target}", String.valueOf(targetAmount))
                                                .replace("{amount}", String.valueOf(targetAmount));
        
        DailyQuest.DailyReward reward = new DailyQuest.DailyReward();
        
        // Randomize reward amounts between min and max
        int money = template.moneyMin == template.moneyMax ? template.moneyMin : 
                    random.nextInt(template.moneyMin, template.moneyMax + 1);
        int exp = template.expMin == template.expMax ? template.expMin : 
                  random.nextInt(template.expMin, template.expMax + 1);
        
        reward.setMoney(money);
        reward.setExperience(exp);
        reward.setQuestPoints(1); // Standard 1 QP for daily quests
        reward.setCommands(template.commandRewards);
        
        // Parse item rewards
        if (template.itemRewards != null && !template.itemRewards.isEmpty()) {
            List<DailyQuest.DailyReward.RewardItem> items = new ArrayList<>();
            for (String itemStr : template.itemRewards) {
                try {
                    String[] parts = itemStr.split(" ");
                    if (parts.length >= 2) {
                        Material material = Material.valueOf(parts[0]);
                        int amount = Integer.parseInt(parts[1]);
                        
                        DailyQuest.DailyReward.RewardItem item = new DailyQuest.DailyReward.RewardItem();
                        item.setMaterial(material);
                        item.setAmount(amount);
                        items.add(item);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid item reward: " + itemStr);
                }
            }
            reward.setItems(items);
        }
        
        DailyQuest quest = new DailyQuest(
            template.id + "_" + System.currentTimeMillis(),
            template.name,
            description,
            template.type,
            template.displayMaterial,
            template.target,
            targetAmount,
            reward
        );
        
        // Store requirements in the quest for checking
        quest.setRequirements(template.requirements);
        
        return quest;
    }
    
    public void addProgress(UUID playerId, DailyQuest.QuestType type, String target, int amount) {
        List<DailyQuest> quests = getPlayerDailyQuests(playerId);
        
        for (DailyQuest quest : quests) {
            if (quest.getType() == type && !quest.isCompleted()) {
                // Check if the target matches quest requirements
                boolean matchesTarget = matchesQuestTarget(quest, target);
                
                if (matchesTarget) {
                // Store old progress percentage for throttling
                int oldPercentage = quest.getProgressPercentage();
                quest.addProgress(amount);
                int newPercentage = quest.getProgressPercentage();
                
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    if (quest.isCompleted()) {
                        // Always show completion
                        plugin.getBossBarManager().showQuestCompleted(player, quest);
                    } else {
                        // Only show progress if it increased by 5% or more
                        if (shouldShowProgressUpdate(oldPercentage, newPercentage)) {
                            plugin.getBossBarManager().showQuestProgress(player, quest);
                        }
                    }
                }
                
                // Save progress to database
                // TODO: Save quest progress to database
                }
            }
        }
    }
    
    private boolean matchesQuestTarget(DailyQuest quest, String target) {
        String questTarget = quest.getTarget();
        
        // NEVER match "ANY" - this was causing all quests to progress!
        // "ANY" should only be used for generic category quests that explicitly need it
        
        // Exact match with quest's primary target
        if (questTarget.equals(target)) {
            return true;
        }
        
        // Check if target is in the requirements list
        Map<String, Object> requirements = quest.getRequirements();
        if (requirements != null) {
            // For SMELTING quests with from_smelting requirement, ONLY match if the quest type is SMELTING
            // This prevents mining STONE with silk touch from triggering "smelt stone" quests
            if (requirements.containsKey("from_smelting") && (boolean) requirements.get("from_smelting")) {
                // Smelting quests should ONLY be triggered by the FurnaceExtractEvent
                // which calls addProgress with QuestType.SMELTING
                // If we're here with a different quest type (like MINING), don't match
                if (quest.getType() != DailyQuest.QuestType.SMELTING) {
                    return false;
                }
            }
            
            // Check materials list (for mining, woodcutting, farming, building, smelting)
            if (requirements.containsKey("materials")) {
                List<String> materials = (List<String>) requirements.get("materials");
                if (materials != null && materials.contains(target)) {
                    return true;
                }
            }
            
            // Check mob_types list (for hunting quests - mob kills)
            if (requirements.containsKey("mob_types")) {
                List<String> mobTypes = (List<String>) requirements.get("mob_types");
                if (mobTypes != null && mobTypes.contains(target)) {
                    return true;
                }
            }
            
            // Check items list (for hunting quests - item collection)
            if (requirements.containsKey("items")) {
                List<String> items = (List<String>) requirements.get("items");
                if (items != null && items.contains(target)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private DailyQuest.QuestType parseQuestType(String typeStr) {
        try {
            return DailyQuest.QuestType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid quest type: " + typeStr + ", defaulting to MINING");
            return DailyQuest.QuestType.MINING;
        }
    }
    
    /**
     * Converts crop block materials to their item equivalents
     * @param materialName The material name to convert
     * @return The converted material name
     */
    private String convertCropBlockToItem(String materialName) {
        return switch (materialName) {
            case "BEETROOTS" -> "BEETROOT";
            case "CARROTS" -> "CARROT";
            case "POTATOES" -> "POTATO";
            case "SWEET_BERRY_BUSH" -> "SWEET_BERRIES";
            case "COCOA" -> "COCOA_BEANS";
            case "NETHER_WART" -> "NETHER_WART"; // Same name
            default -> materialName; // Return as-is
        };
    }
    
    public boolean claimReward(UUID playerId, String questId) {
        List<DailyQuest> quests = getPlayerDailyQuests(playerId);
        
        for (DailyQuest quest : quests) {
            if (quest.getId().equals(questId) && quest.canClaim()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    // Give rewards
                    DailyQuest.DailyReward reward = quest.getReward();
                    
                    // Money reward
                    if (reward.getMoney() > 0 && plugin.getVaultIntegration() != null) {
                        plugin.getVaultIntegration().deposit(player, reward.getMoney());
                        player.sendMessage(plugin.parseMessage("<gray>[<green><bold>$</bold></green><gray>]<reset> Received <green>$" + reward.getMoney() + "<reset>!"));
                    }
                    
                    // Experience reward
                    if (reward.getExperience() > 0) {
                        player.giveExp(reward.getExperience());
                        player.sendMessage(plugin.parseMessage("<gray>[<aqua><bold>✦</bold></aqua><gray>]<reset> Received <aqua>" + reward.getExperience() + " XP<reset>!"));
                    }
                    
                    // Quest Points reward (standardized: 1 QP for daily quests)
                    String qpCommand = "qp give " + player.getName() + " 1";
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), qpCommand);
                    player.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>★</bold></light_purple><gray>]<reset> Received <light_purple>1 Quest Point<reset>!"));
                    
                    // Command-based rewards for maximum flexibility
                    if (reward.getCommands() != null && !reward.getCommands().isEmpty()) {
                        for (String command : reward.getCommands()) {
                            String processedCommand = command.replace("{player}", player.getName())
                                                           .replace("{amount}", String.valueOf(reward.getQuestPoints()));
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                        }
                    }
                    
                    // Item rewards
                    if (reward.getItems() != null && !reward.getItems().isEmpty()) {
                        for (DailyQuest.DailyReward.RewardItem rewardItem : reward.getItems()) {
                            // TODO: Give items to player - rewardItem contains material and amount
                        }
                    }
                    
                    quest.claim();
                    player.sendMessage(plugin.parseMessage("<gray>[<green><bold>✓</bold></green><gray>]<reset> Claimed reward for: <yellow>" + quest.getName() + "<reset>!"));
                    
                    // Save to database
                    // TODO: Save claimed status to database
                    
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void startDailyResetScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForDailyReset();
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L); // Check every minute
    }
    
    private void checkForDailyReset() {
        LocalDateTime now = LocalDateTime.now();
        
        // Check if it's midnight (or close to it)
        if (now.getHour() == 0 && now.getMinute() == 0) {
            plugin.getLogger().info("Performing daily quest reset...");
            
            // Reset all player daily quests
            for (UUID playerId : playerDailyQuests.keySet()) {
                generateNewDailyQuestsForPlayer(playerId);
            }
            
            plugin.getLogger().info("Daily quest reset completed!");
        }
    }
    
    private boolean shouldShowProgressUpdate(int oldPercentage, int newPercentage) {
        // Always show first progress (0% -> any%)
        if (oldPercentage == 0 && newPercentage > 0) {
            return true;
        }
        
        // Show every 10% milestone
        int oldMilestone = oldPercentage / 10;
        int newMilestone = newPercentage / 10;
        
        return newMilestone > oldMilestone;
    }
    
    // Template class for quest generation
    private static class DailyQuestTemplate {
        final String id;
        final String name;
        final String description;
        final DailyQuest.QuestType type;
        final Material displayMaterial;
        final String target;
        final int minAmount;
        final int maxAmount;
        final int moneyMin;
        final int moneyMax;
        final int expMin;
        final int expMax;
        final List<String> itemRewards;
        final List<String> commandRewards;
        final int weight;
        final Map<String, Object> requirements;
        
        DailyQuestTemplate(String id, String name, String description, DailyQuest.QuestType type,
                          Material displayMaterial, String target, int minAmount, int maxAmount,
                          int moneyMin, int moneyMax, int expMin, int expMax,
                          List<String> itemRewards, List<String> commandRewards,
                          int weight, Map<String, Object> requirements) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.displayMaterial = displayMaterial;
            this.target = target;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.moneyMin = moneyMin;
            this.moneyMax = moneyMax;
            this.expMin = expMin;
            this.expMax = expMax;
            this.itemRewards = itemRewards;
            this.commandRewards = commandRewards;
            this.weight = weight;
            this.requirements = requirements;
        }
    }
}
