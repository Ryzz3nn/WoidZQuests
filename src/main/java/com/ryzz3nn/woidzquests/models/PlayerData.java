package com.ryzz3nn.woidzquests.models;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerData {
    
    private UUID uuid;
    private String username;
    
    private int questPoints;
    private int dailyRerolls;
    private int weeklyRerolls;
    
    private LocalDateTime lastDailyReset;
    private LocalDateTime lastWeeklyReset;
    private LocalDateTime joinDate;
    private LocalDateTime lastSeen;
    
    private int totalQuestsCompleted;
    
    @Builder.Default
    private Map<String, Integer> statistics = new HashMap<>();
    
    @Builder.Default
    private Map<String, Object> settings = new HashMap<>();
    
    public void addQuestPoints(int points) {
        this.questPoints += points;
    }
    
    public void removeQuestPoints(int points) {
        this.questPoints = Math.max(0, this.questPoints - points);
    }
    
    public boolean canRerollDaily() {
        return dailyRerolls > 0;
    }
    
    public boolean canRerollWeekly() {
        return weeklyRerolls > 0;
    }
    
    public void useReroll(boolean isDaily) {
        if (isDaily && dailyRerolls > 0) {
            dailyRerolls--;
        } else if (!isDaily && weeklyRerolls > 0) {
            weeklyRerolls--;
        }
    }
    
    public void resetDailyRerolls(int amount) {
        this.dailyRerolls = amount;
        this.lastDailyReset = LocalDateTime.now();
    }
    
    public void resetWeeklyRerolls(int amount) {
        this.weeklyRerolls = amount;
        this.lastWeeklyReset = LocalDateTime.now();
    }
    
    public void incrementQuestsCompleted() {
        this.totalQuestsCompleted++;
    }
    
    public void addStatistic(String key, int value) {
        statistics.put(key, statistics.getOrDefault(key, 0) + value);
    }
    
    public int getStatistic(String key) {
        return statistics.getOrDefault(key, 0);
    }
    
    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }
    
    public <T> T getSetting(String key, T defaultValue) {
        Object value = settings.get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    public boolean needsDailyReset() {
        if (lastDailyReset == null) return true;
        LocalDateTime now = LocalDateTime.now();
        return !lastDailyReset.toLocalDate().equals(now.toLocalDate());
    }
    
    public boolean needsWeeklyReset() {
        if (lastWeeklyReset == null) return true;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
        return lastWeeklyReset.isBefore(weekStart);
    }
}
