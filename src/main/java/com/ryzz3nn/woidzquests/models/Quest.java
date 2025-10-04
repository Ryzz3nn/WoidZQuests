package com.ryzz3nn.woidzquests.models;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Quest {
    
    private String id;
    private UUID playerUuid;
    private String questId;
    private QuestType type;
    private QuestStatus status;
    private String name;
    private String description;
    private QuestCategory category;
    private QuestDifficulty difficulty;
    
    private int progress;
    private int target;
    private boolean tracked;
    
    private Map<String, Object> requirements;
    private Map<String, Object> rewards;
    private Map<String, Object> questData;
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    
    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }
    
    public boolean isActive() {
        return status == QuestStatus.ACTIVE;
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public double getProgressPercentage() {
        if (target <= 0) return 0.0;
        return Math.min(100.0, (double) progress / target * 100.0);
    }
    
    public void addProgress(int amount) {
        this.progress = Math.min(target, progress + amount);
        if (this.progress >= target && status == QuestStatus.ACTIVE) {
            this.status = QuestStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public void setProgress(int progress) {
        this.progress = Math.min(target, Math.max(0, progress));
        if (this.progress >= target && status == QuestStatus.ACTIVE) {
            this.status = QuestStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public boolean canProgress() {
        return status == QuestStatus.ACTIVE && !isExpired() && progress < target;
    }
    
    public enum QuestStatus {
        ACTIVE,
        COMPLETED,
        CLAIMED,
        EXPIRED,
        CANCELLED
    }
    
    public enum QuestType {
        MINING,
        WOODCUTTING,
        FARMING,
        FISHING,
        HUNTING,
        BUILDING,
        CRAFTING,
        SMELTING,
        BREWING,
        ENCHANTING,
        ANVIL,
        SMITHING,
        EXPLORATION,
        SURVIVAL,
        JOBS_XP,
        JOBS_MONEY,
        JOBS_LEVEL,
        TIMED,
        GLOBAL
    }
    
    public enum QuestCategory {
        GATHERING,
        COMBAT,
        CRAFTING,
        EXPLORATION,
        SURVIVAL,
        JOBS,
        BUILDING,
        SPECIAL
    }
    
    public enum QuestDifficulty {
        EASY(1.0, "&a&lEasy"),
        MEDIUM(1.5, "&e&lMedium"),
        HARD(2.0, "&c&lHard"),
        EXTREME(3.0, "&4&lExtreme");
        
        private final double multiplier;
        private final String displayName;
        
        QuestDifficulty(double multiplier, String displayName) {
            this.multiplier = multiplier;
            this.displayName = displayName;
        }
        
        public double getMultiplier() {
            return multiplier;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
