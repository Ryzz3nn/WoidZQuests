package com.ryzz3nn.woidzquests.managers;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.database.DatabaseManager;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Getter
public class PlayerDataManager {
    
    private final WoidZQuests plugin;
    private final DatabaseManager databaseManager;
    private final Gson gson;
    private final Map<UUID, PlayerData> playerDataCache;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public PlayerDataManager(WoidZQuests plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.gson = new Gson();
        this.playerDataCache = new ConcurrentHashMap<>();
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, this::loadPlayerData);
    }
    
    public PlayerData loadPlayerData(UUID uuid) {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = """
                SELECT uuid, username, quest_points, daily_rerolls, weekly_rerolls,
                       last_daily_reset, last_weekly_reset, total_quests_completed,
                       join_date, last_seen
                FROM player_data WHERE uuid = ?
            """;
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return PlayerData.builder()
                            .uuid(uuid)
                            .username(resultSet.getString("username"))
                            .questPoints(resultSet.getInt("quest_points"))
                            .dailyRerolls(resultSet.getInt("daily_rerolls"))
                            .weeklyRerolls(resultSet.getInt("weekly_rerolls"))
                            .lastDailyReset(parseDateTime(resultSet.getString("last_daily_reset")))
                            .lastWeeklyReset(parseDateTime(resultSet.getString("last_weekly_reset")))
                            .totalQuestsCompleted(resultSet.getInt("total_quests_completed"))
                            .joinDate(parseDateTime(resultSet.getString("join_date")))
                            .lastSeen(parseDateTime(resultSet.getString("last_seen")))
                            .statistics(loadPlayerStatistics(uuid))
                            .settings(new HashMap<>())
                            .build();
                    }
                }
            }
            
            // Create new player data if not found
            return createNewPlayerData(uuid);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            return createNewPlayerData(uuid);
        }
    }
    
    private PlayerData createNewPlayerData(UUID uuid) {
        LocalDateTime now = LocalDateTime.now();
        return PlayerData.builder()
            .uuid(uuid)
            .username("Unknown")
            .questPoints(0)
            .dailyRerolls(plugin.getConfigManager().getDailyQuestRerolls())
            .weeklyRerolls(plugin.getConfigManager().getInt("quests.weekly-rerolls", 1))
            .lastDailyReset(now)
            .lastWeeklyReset(now)
            .totalQuestsCompleted(0)
            .joinDate(now)
            .lastSeen(now)
            .statistics(new HashMap<>())
            .settings(new HashMap<>())
            .build();
    }
    
    public void savePlayerData(PlayerData playerData) {
        databaseManager.executeAsync(connection -> {
            String sql = """
                INSERT OR REPLACE INTO player_data 
                (uuid, username, quest_points, daily_rerolls, weekly_rerolls,
                 last_daily_reset, last_weekly_reset, total_quests_completed,
                 join_date, last_seen, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerData.getUuid().toString());
                statement.setString(2, playerData.getUsername());
                statement.setInt(3, playerData.getQuestPoints());
                statement.setInt(4, playerData.getDailyRerolls());
                statement.setInt(5, playerData.getWeeklyRerolls());
                statement.setString(6, formatDateTime(playerData.getLastDailyReset()));
                statement.setString(7, formatDateTime(playerData.getLastWeeklyReset()));
                statement.setInt(8, playerData.getTotalQuestsCompleted());
                statement.setString(9, formatDateTime(playerData.getJoinDate()));
                statement.setString(10, formatDateTime(playerData.getLastSeen()));
                
                statement.executeUpdate();
                
                // Save statistics
                savePlayerStatistics(playerData.getUuid(), playerData.getStatistics());
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerData.getUuid(), e);
            }
        });
    }
    
    private Map<String, Integer> loadPlayerStatistics(UUID uuid) {
        Map<String, Integer> statistics = new HashMap<>();
        
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT stat_type, stat_value FROM player_statistics WHERE player_uuid = ?";
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        statistics.put(resultSet.getString("stat_type"), resultSet.getInt("stat_value"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player statistics for " + uuid, e);
        }
        
        return statistics;
    }
    
    private void savePlayerStatistics(UUID uuid, Map<String, Integer> statistics) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO player_statistics (player_uuid, stat_type, stat_value, last_updated)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        """;
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            for (Map.Entry<String, Integer> entry : statistics.entrySet()) {
                statement.setString(1, uuid.toString());
                statement.setString(2, entry.getKey());
                statement.setInt(3, entry.getValue());
                statement.addBatch();
            }
            
            statement.executeBatch();
        }
    }
    
    public void saveAllPlayerData() {
        plugin.getLogger().info("Saving all player data...");
        
        for (PlayerData playerData : playerDataCache.values()) {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = """
                    INSERT OR REPLACE INTO player_data 
                    (uuid, username, quest_points, daily_rerolls, weekly_rerolls,
                     last_daily_reset, last_weekly_reset, total_quests_completed,
                     join_date, last_seen, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, playerData.getUuid().toString());
                    statement.setString(2, playerData.getUsername());
                    statement.setInt(3, playerData.getQuestPoints());
                    statement.setInt(4, playerData.getDailyRerolls());
                    statement.setInt(5, playerData.getWeeklyRerolls());
                    statement.setString(6, formatDateTime(playerData.getLastDailyReset()));
                    statement.setString(7, formatDateTime(playerData.getLastWeeklyReset()));
                    statement.setInt(8, playerData.getTotalQuestsCompleted());
                    statement.setString(9, formatDateTime(playerData.getJoinDate()));
                    statement.setString(10, formatDateTime(playerData.getLastSeen()));
                    
                    statement.executeUpdate();
                    
                    // Save statistics
                    savePlayerStatistics(playerData.getUuid(), playerData.getStatistics());
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerData.getUuid(), e);
            }
        }
        
        plugin.getLogger().info("Player data saved successfully!");
    }
    
    public void resetDailyData() {
        LocalDateTime now = LocalDateTime.now();
        int dailyRerolls = plugin.getConfigManager().getDailyQuestRerolls();
        
        for (PlayerData playerData : playerDataCache.values()) {
            if (playerData.needsDailyReset()) {
                playerData.resetDailyRerolls(dailyRerolls);
                savePlayerData(playerData);
            }
        }
    }
    
    public void resetWeeklyData() {
        LocalDateTime now = LocalDateTime.now();
        int weeklyRerolls = plugin.getConfigManager().getInt("quests.weekly-rerolls", 1);
        
        for (PlayerData playerData : playerDataCache.values()) {
            if (playerData.needsWeeklyReset()) {
                playerData.resetWeeklyRerolls(weeklyRerolls);
                savePlayerData(playerData);
            }
        }
    }
    
    public void removePlayerData(UUID uuid) {
        playerDataCache.remove(uuid);
    }
    
    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateTimeString, dateFormatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return LocalDateTime.now().format(dateFormatter);
        }
        return dateTime.format(dateFormatter);
    }
}
