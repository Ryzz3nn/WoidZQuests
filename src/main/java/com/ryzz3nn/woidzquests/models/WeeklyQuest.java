package com.ryzz3nn.woidzquests.models;

import lombok.Data;
import org.bukkit.Material;

@Data
public class WeeklyQuest {
    private String id;
    private String name;
    private String description;
    private String type; // MINING, FISHING, COMBAT, FARMING, BUILDING, etc.
    private String target; // Material name, mob type, etc.
    private int targetAmount;
    private int currentProgress;
    private boolean completed;
    private boolean claimed;
    private Material displayMaterial;
    private WeeklyReward reward;

    public WeeklyQuest(String id, String name, String description, String type, String target, 
                      int targetAmount, Material displayMaterial, WeeklyReward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.target = target;
        this.targetAmount = targetAmount;
        this.currentProgress = 0;
        this.completed = false;
        this.claimed = false;
        this.displayMaterial = displayMaterial;
        this.reward = reward;
    }

    public void addProgress(int amount) {
        if (!completed) {
            this.currentProgress += amount;
            if (this.currentProgress >= this.targetAmount) {
                this.currentProgress = this.targetAmount;
                this.completed = true;
            }
        }
    }

    public int getProgressPercentage() {
        if (targetAmount == 0) return 0;
        return Math.min(100, (currentProgress * 100) / targetAmount);
    }

    public String getProgressColor() {
        int percentage = getProgressPercentage();
        if (percentage >= 100) return "<gold>"; // Gold for completion
        if (percentage >= 75) return "<green>";
        if (percentage >= 50) return "<yellow>";
        if (percentage >= 25) return "<#FFA500>"; // Orange hex color
        return "<red>"; // Low progress
    }
    
    public boolean canClaim() {
        return completed && !claimed;
    }

    @Data
    public static class WeeklyReward {
        private double money;
        private int experience;
        private int questPoints;
        private java.util.List<String> commands; // Command-based rewards for flexibility

        public WeeklyReward(double money, int experience, int questPoints) {
            this.money = money;
            this.experience = experience;
            this.questPoints = questPoints;
            this.commands = new java.util.ArrayList<>();
        }
    }
}
