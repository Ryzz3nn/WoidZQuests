package com.ryzz3nn.woidzquests.placeholders;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.Quest;
import com.ryzz3nn.woidzquests.models.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class QuestPlaceholders extends PlaceholderExpansion {
    
    private final WoidZQuests plugin;
    
    public QuestPlaceholders(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "woidzquests";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "Ryzz3nn";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        List<Quest> activeQuests = plugin.getQuestManager().getActiveQuests(player.getUniqueId());
        List<Quest> completedQuests = plugin.getQuestManager().getCompletedQuests(player.getUniqueId());
        
        // Player data placeholders
        if (params.equals("quest_points")) {
            return String.valueOf(playerData.getQuestPoints());
        }
        
        if (params.equals("daily_rerolls")) {
            return String.valueOf(playerData.getDailyRerolls());
        }
        
        if (params.equals("weekly_rerolls")) {
            return String.valueOf(playerData.getWeeklyRerolls());
        }
        
        if (params.equals("total_completed")) {
            return String.valueOf(playerData.getTotalQuestsCompleted());
        }
        
        // Quest count placeholders
        if (params.equals("active_count")) {
            return String.valueOf(activeQuests.size());
        }
        
        if (params.equals("completed_count")) {
            return String.valueOf(completedQuests.size());
        }
        
        if (params.equals("max_active")) {
            return String.valueOf(plugin.getConfigManager().getMaxActiveQuests());
        }
        
        // Current tracked quest placeholders
        Quest trackedQuest = activeQuests.stream()
            .filter(Quest::isTracked)
            .findFirst()
            .orElse(null);
        
        if (trackedQuest != null) {
            if (params.equals("tracked_name")) {
                return trackedQuest.getName();
            }
            
            if (params.equals("tracked_progress")) {
                return String.valueOf(trackedQuest.getProgress());
            }
            
            if (params.equals("tracked_target")) {
                return String.valueOf(trackedQuest.getTarget());
            }
            
            if (params.equals("tracked_percentage")) {
                return String.format("%.1f", trackedQuest.getProgressPercentage());
            }
            
            if (params.equals("tracked_description")) {
                return trackedQuest.getDescription();
            }
        } else {
            if (params.equals("tracked_name")) {
                return "No tracked quest";
            }
            
            if (params.equals("tracked_progress") || params.equals("tracked_target")) {
                return "0";
            }
            
            if (params.equals("tracked_percentage")) {
                return "0.0";
            }
            
            if (params.equals("tracked_description")) {
                return "No quest being tracked";
            }
        }
        
        // Quest by index placeholders (quest_1_name, quest_1_progress, etc.)
        if (params.startsWith("quest_") && params.contains("_")) {
            String[] parts = params.split("_");
            if (parts.length >= 3) {
                try {
                    int index = Integer.parseInt(parts[1]) - 1; // 1-based to 0-based
                    String property = parts[2];
                    
                    if (index >= 0 && index < activeQuests.size()) {
                        Quest quest = activeQuests.get(index);
                        
                        return switch (property) {
                            case "name" -> quest.getName();
                            case "progress" -> String.valueOf(quest.getProgress());
                            case "target" -> String.valueOf(quest.getTarget());
                            case "percentage" -> String.format("%.1f", quest.getProgressPercentage());
                            case "description" -> quest.getDescription();
                            case "type" -> quest.getType().name();
                            case "difficulty" -> quest.getDifficulty() != null ? quest.getDifficulty().name() : "UNKNOWN";
                            case "category" -> quest.getCategory() != null ? quest.getCategory().name() : "UNKNOWN";
                            default -> "";
                        };
                    }
                } catch (NumberFormatException ignored) {
                    // Invalid number format
                }
            }
        }
        
        // Statistics placeholders
        if (params.startsWith("stat_")) {
            String statName = params.substring(5);
            return String.valueOf(playerData.getStatistic(statName));
        }
        
        return null; // Placeholder not found
    }
}
