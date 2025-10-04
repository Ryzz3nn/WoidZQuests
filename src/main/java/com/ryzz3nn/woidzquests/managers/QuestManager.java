package com.ryzz3nn.woidzquests.managers;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.database.DatabaseManager;
import com.ryzz3nn.woidzquests.models.Quest;
import com.ryzz3nn.woidzquests.models.PlayerData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Getter
public class QuestManager {
    
    private final WoidZQuests plugin;
    private final DatabaseManager databaseManager;
    private final Gson gson;
    private final Map<UUID, List<Quest>> playerQuests;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public QuestManager(WoidZQuests plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.gson = new Gson();
        this.playerQuests = new ConcurrentHashMap<>();
    }
    
    public boolean initialize() {
        try {
            // Load quest definitions from config or database
            loadQuestDefinitions();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize quest manager", e);
            return false;
        }
    }
    
    public void reload() {
        playerQuests.clear();
        loadQuestDefinitions();
    }
    
    private void loadQuestDefinitions() {
        // This would load quest definitions from config files or database
        // For now, we'll create some basic quest definitions
        plugin.getLogger().info("Quest definitions loaded successfully");
    }
    
    public List<Quest> getPlayerQuests(UUID playerUuid) {
        return playerQuests.computeIfAbsent(playerUuid, this::loadPlayerQuests);
    }
    
    public List<Quest> getActiveQuests(UUID playerUuid) {
        return getPlayerQuests(playerUuid).stream()
            .filter(Quest::isActive)
            .toList();
    }
    
    public List<Quest> getCompletedQuests(UUID playerUuid) {
        return getPlayerQuests(playerUuid).stream()
            .filter(Quest::isCompleted)
            .toList();
    }
    
    private List<Quest> loadPlayerQuests(UUID playerUuid) {
        List<Quest> quests = new ArrayList<>();
        
        try (Connection connection = databaseManager.getConnection()) {
            String sql = """
                SELECT id, quest_id, quest_type, quest_data, progress, target, status,
                       is_tracked, started_at, completed_at, expires_at
                FROM player_quests WHERE player_uuid = ?
            """;
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Map<String, Object> questData = gson.fromJson(
                            resultSet.getString("quest_data"),
                            new TypeToken<Map<String, Object>>(){}.getType()
                        );
                        
                        Quest quest = Quest.builder()
                            .id(resultSet.getString("id"))
                            .playerUuid(playerUuid)
                            .questId(resultSet.getString("quest_id"))
                            .type(Quest.QuestType.valueOf(resultSet.getString("quest_type")))
                            .status(Quest.QuestStatus.valueOf(resultSet.getString("status")))
                            .progress(resultSet.getInt("progress"))
                            .target(resultSet.getInt("target"))
                            .tracked(resultSet.getBoolean("is_tracked"))
                            .questData(questData)
                            .startedAt(parseDateTime(resultSet.getString("started_at")))
                            .completedAt(parseDateTime(resultSet.getString("completed_at")))
                            .expiresAt(parseDateTime(resultSet.getString("expires_at")))
                            .build();
                        
                        // Set additional quest properties from quest data
                        if (questData != null) {
                            quest.setName((String) questData.getOrDefault("name", "Unknown Quest"));
                            quest.setDescription((String) questData.getOrDefault("description", "No description"));
                            
                            String categoryStr = (String) questData.get("category");
                            if (categoryStr != null) {
                                quest.setCategory(Quest.QuestCategory.valueOf(categoryStr));
                            }
                            
                            String difficultyStr = (String) questData.get("difficulty");
                            if (difficultyStr != null) {
                                quest.setDifficulty(Quest.QuestDifficulty.valueOf(difficultyStr));
                            }
                            
                            quest.setRequirements((Map<String, Object>) questData.get("requirements"));
                            quest.setRewards((Map<String, Object>) questData.get("rewards"));
                        }
                        
                        quests.add(quest);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player quests for " + playerUuid, e);
        }
        
        return quests;
    }
    
    public void saveQuest(Quest quest) {
        databaseManager.executeAsync(connection -> {
            String sql = """
                INSERT OR REPLACE INTO player_quests 
                (id, player_uuid, quest_id, quest_type, quest_data, progress, target, status,
                 is_tracked, started_at, completed_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                // Prepare quest data
                Map<String, Object> questData = quest.getQuestData() != null ? quest.getQuestData() : new HashMap<>();
                questData.put("name", quest.getName());
                questData.put("description", quest.getDescription());
                questData.put("category", quest.getCategory() != null ? quest.getCategory().name() : null);
                questData.put("difficulty", quest.getDifficulty() != null ? quest.getDifficulty().name() : null);
                questData.put("requirements", quest.getRequirements());
                questData.put("rewards", quest.getRewards());
                
                statement.setString(1, quest.getId());
                statement.setString(2, quest.getPlayerUuid().toString());
                statement.setString(3, quest.getQuestId());
                statement.setString(4, quest.getType().name());
                statement.setString(5, gson.toJson(questData));
                statement.setInt(6, quest.getProgress());
                statement.setInt(7, quest.getTarget());
                statement.setString(8, quest.getStatus().name());
                statement.setBoolean(9, quest.isTracked());
                statement.setString(10, formatDateTime(quest.getStartedAt()));
                statement.setString(11, formatDateTime(quest.getCompletedAt()));
                statement.setString(12, formatDateTime(quest.getExpiresAt()));
                
                statement.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save quest " + quest.getId(), e);
            }
        });
    }
    
    public boolean addProgress(UUID playerUuid, Quest.QuestType questType, Map<String, Object> progressData, int amount) {
        List<Quest> quests = getActiveQuests(playerUuid);
        boolean progressMade = false;
        
        for (Quest quest : quests) {
            if (quest.getType() == questType && quest.canProgress()) {
                if (meetsRequirements(quest, progressData)) {
                    quest.addProgress(amount);
                    saveQuest(quest);
                    
                    // Send progress message
                    if (quest.isTracked()) {
                        sendProgressMessage(playerUuid, quest);
                    }
                    
                    // Check if quest is completed
                    if (quest.isCompleted()) {
                        onQuestCompleted(quest);
                    }
                    
                    progressMade = true;
                }
            }
        }
        
        return progressMade;
    }
    
    private boolean meetsRequirements(Quest quest, Map<String, Object> progressData) {
        Map<String, Object> requirements = quest.getRequirements();
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        
        // Check material requirements
        if (requirements.containsKey("materials")) {
            List<String> requiredMaterials = (List<String>) requirements.get("materials");
            String material = (String) progressData.get("material");
            if (material != null && !requiredMaterials.contains(material)) {
                return false;
            }
        }
        
        // Check world requirements
        if (requirements.containsKey("worlds")) {
            List<String> requiredWorlds = (List<String>) requirements.get("worlds");
            String world = (String) progressData.get("world");
            if (world != null && !requiredWorlds.contains(world)) {
                return false;
            }
        }
        
        // Check biome requirements
        if (requirements.containsKey("biomes")) {
            List<String> requiredBiomes = (List<String>) requirements.get("biomes");
            String biome = (String) progressData.get("biome");
            if (biome != null && !requiredBiomes.contains(biome)) {
                return false;
            }
        }
        
        // Check Y-level requirements
        if (requirements.containsKey("y_min") || requirements.containsKey("y_max")) {
            Integer y = (Integer) progressData.get("y");
            if (y != null) {
                Integer yMin = (Integer) requirements.get("y_min");
                Integer yMax = (Integer) requirements.get("y_max");
                
                if (yMin != null && y < yMin) return false;
                if (yMax != null && y > yMax) return false;
            }
        }
        
        return true;
    }
    
    private void sendProgressMessage(UUID playerUuid, Quest quest) {
        // Send progress message to player
        // This would use the messaging system
    }
    
    private void onQuestCompleted(Quest quest) {
        // Handle quest completion
        plugin.getLogger().info("Quest completed: " + quest.getName() + " by " + quest.getPlayerUuid());
        
        // Update player statistics
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(quest.getPlayerUuid());
        playerData.incrementQuestsCompleted();
        plugin.getPlayerDataManager().savePlayerData(playerData);
        
        // Send completion message
        // This would use the messaging system
    }
    
    public Quest createQuest(UUID playerUuid, Quest.QuestType type, String name, String description, 
                           Quest.QuestCategory category, Quest.QuestDifficulty difficulty,
                           int target, Map<String, Object> requirements, Map<String, Object> rewards) {
        
        String questId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        Quest quest = Quest.builder()
            .id(questId)
            .playerUuid(playerUuid)
            .questId(questId)
            .type(type)
            .status(Quest.QuestStatus.ACTIVE)
            .name(name)
            .description(description)
            .category(category)
            .difficulty(difficulty)
            .progress(0)
            .target(target)
            .tracked(false)
            .requirements(requirements)
            .rewards(rewards)
            .questData(new HashMap<>())
            .startedAt(now)
            .build();
        
        // Add to player's quest list
        getPlayerQuests(playerUuid).add(quest);
        
        // Save to database
        saveQuest(quest);
        
        return quest;
    }
    
    public void removeQuest(UUID playerUuid, String questId) {
        List<Quest> quests = getPlayerQuests(playerUuid);
        quests.removeIf(quest -> quest.getId().equals(questId));
        
        // Remove from database
        databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM player_quests WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, questId);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove quest " + questId, e);
            }
        });
    }
    
    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeString, dateFormatter);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(dateFormatter);
    }
}
