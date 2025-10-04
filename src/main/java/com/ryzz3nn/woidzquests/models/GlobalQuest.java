package com.ryzz3nn.woidzquests.models;

import lombok.Data;
import org.bukkit.Material;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class GlobalQuest {
    private String id;
    private String name;
    private String description;
    private String type; // MINING, FISHING, COMBAT, FARMING, BUILDING, etc.
    private String target; // Material name, mob type, etc.
    private long targetAmount; // Large numbers for server-wide goals
    private long currentProgress;
    private boolean completed;
    private Material displayMaterial;
    private GlobalReward reward;
    private Map<UUID, Long> playerContributions; // Track individual contributions
    private Map<UUID, Boolean> playerClaimed; // Track who claimed rewards

    public GlobalQuest(String id, String name, String description, String type, String target, 
                      long targetAmount, Material displayMaterial, GlobalReward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.target = target;
        this.targetAmount = targetAmount;
        this.currentProgress = 0;
        this.completed = false;
        this.displayMaterial = displayMaterial;
        this.reward = reward;
        this.playerContributions = new HashMap<>();
        this.playerClaimed = new HashMap<>();
    }

    public void addProgress(UUID playerId, long amount) {
        if (!completed) {
            this.currentProgress += amount;
            
            // Track individual player contribution
            playerContributions.put(playerId, playerContributions.getOrDefault(playerId, 0L) + amount);
            
            if (this.currentProgress >= this.targetAmount) {
                this.currentProgress = this.targetAmount;
                this.completed = true;
            }
        }
    }

    public long getPlayerContribution(UUID playerId) {
        return playerContributions.getOrDefault(playerId, 0L);
    }

    public boolean hasPlayerClaimed(UUID playerId) {
        return playerClaimed.getOrDefault(playerId, false);
    }

    public void setPlayerClaimed(UUID playerId, boolean claimed) {
        playerClaimed.put(playerId, claimed);
    }

    public int getProgressPercentage() {
        if (targetAmount == 0) return 0;
        return (int) Math.min(100, (currentProgress * 100) / targetAmount);
    }

    public String getProgressColor() {
        int percentage = getProgressPercentage();
        if (percentage >= 100) return "<gold>"; // Gold for completion
        if (percentage >= 75) return "<green>";
        if (percentage >= 50) return "<yellow>";
        if (percentage >= 25) return "<#FFA500>"; // Orange hex color
        return "<red>"; // Low progress
    }

    public String getFormattedProgress() {
        return formatLargeNumber(currentProgress);
    }

    public String getFormattedTarget() {
        return formatLargeNumber(targetAmount);
    }

    private String formatLargeNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    @Data
    public static class GlobalReward {
        private double money;
        private int experience;
        private int questPoints;
        private java.util.List<String> commands; // Command-based rewards for flexibility

        public GlobalReward(double money, int experience, int questPoints) {
            this.money = money;
            this.experience = experience;
            this.questPoints = questPoints;
            this.commands = new java.util.ArrayList<>();
        }
    }
}
