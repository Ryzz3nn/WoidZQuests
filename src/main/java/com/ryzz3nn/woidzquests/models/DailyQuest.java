package com.ryzz3nn.woidzquests.models;

import lombok.Data;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

@Data
public class DailyQuest {
    
    private String id;
    private String name;
    private String description;
    private QuestType type;
    private Material displayMaterial;
    private String target;
    private int targetAmount;
    private int currentProgress;
    private boolean completed;
    private boolean claimed;
    private DailyReward reward;
    private long createdDate; // Date when quest was created (for daily reset)
    private Map<String, Object> requirements; // Quest requirements (materials, worlds, biomes, etc.)
    
    public enum QuestType {
        MINING,
        WOODCUTTING,
        FARMING,
        FISHING,
        HUNTING,
        BUILDING,
        CRAFTING,
        EXPLORATION,
        SURVIVAL
    }
    
    @Data
    public static class DailyReward {
        private int money;
        private int experience;
        private int questPoints;
        private List<String> commands; // Command-based rewards for flexibility
        private List<RewardItem> items;
        private int jobXp;
        private String jobName;
        
        @Data
        public static class RewardItem {
            private Material material;
            private int amount;
            private String displayName;
        }
    }
    
    public DailyQuest(String id, String name, String description, QuestType type, 
                     Material displayMaterial, String target, int targetAmount, DailyReward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.displayMaterial = displayMaterial;
        this.target = target;
        this.targetAmount = targetAmount;
        this.currentProgress = 0;
        this.completed = false;
        this.claimed = false;
        this.reward = reward;
        this.createdDate = System.currentTimeMillis();
    }
    
    public void addProgress(int amount) {
        if (!completed) {
            currentProgress += amount;
            if (currentProgress >= targetAmount) {
                currentProgress = targetAmount;
                completed = true;
            }
        }
    }
    
    public int getProgressPercentage() {
        if (targetAmount == 0) return 0;
        return Math.min(100, (currentProgress * 100) / targetAmount);
    }
    
    public String getProgressColor() {
        int percentage = getProgressPercentage();
        if (percentage >= 100) return "<gold>";
        if (percentage >= 75) return "<green>";
        if (percentage >= 50) return "<yellow>";
        if (percentage >= 25) return "<#FFA500>"; // Orange hex color
        return "<red>";
    }
    
    public boolean canClaim() {
        return completed && !claimed;
    }
    
    public void claim() {
        if (canClaim()) {
            claimed = true;
        }
    }
    
    public boolean isExpired() {
        // Check if quest is from previous day (simple check - can be improved)
        long oneDayMs = 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - createdDate) > oneDayMs;
    }
}
