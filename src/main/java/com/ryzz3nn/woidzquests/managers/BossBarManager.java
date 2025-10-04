package com.ryzz3nn.woidzquests.managers;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.models.DailyQuest;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {
    
    private final WoidZQuests plugin;
    private final Map<UUID, BossBar> activeBossBars;
    private final Map<UUID, BukkitRunnable> hideTaskMap;
    
    public BossBarManager(WoidZQuests plugin) {
        this.plugin = plugin;
        this.activeBossBars = new HashMap<>();
        this.hideTaskMap = new HashMap<>();
    }
    
    public void showQuestProgress(Player player, DailyQuest quest) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing hide task
        BukkitRunnable existingTask = hideTaskMap.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Remove existing bossbar if any
        BossBar existingBar = activeBossBars.get(playerId);
        if (existingBar != null) {
            player.hideBossBar(existingBar);
        }
        
        // Create new bossbar
        float progress = Math.min(1.0f, (float) quest.getCurrentProgress() / quest.getTargetAmount());
        BossBar.Color color = getColorForProgress(quest.getProgressPercentage());
        
        Component title = plugin.parseMessage(
            "<gray>" + quest.getName() + " <gray>(<#f79459>" + 
            quest.getCurrentProgress() + "<gray>/<#f79459>" + quest.getTargetAmount() + "<gray>) " +
            quest.getProgressColor() + quest.getProgressPercentage() + "%"
        );
        
        BossBar bossBar = BossBar.bossBar(title, progress, color, BossBar.Overlay.PROGRESS);
        
        // Show bossbar to player
        player.showBossBar(bossBar);
        activeBossBars.put(playerId, bossBar);
        
        // Schedule hide task (hide after 5 seconds)
        BukkitRunnable hideTask = new BukkitRunnable() {
            @Override
            public void run() {
                hideBossBar(player);
            }
        };
        hideTask.runTaskLater(plugin, 100L); // 5 seconds
        hideTaskMap.put(playerId, hideTask);
    }
    
    public void showQuestCompleted(Player player, DailyQuest quest) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing hide task
        BukkitRunnable existingTask = hideTaskMap.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Remove existing bossbar if any
        BossBar existingBar = activeBossBars.get(playerId);
        if (existingBar != null) {
            player.hideBossBar(existingBar);
        }
        
        // Create completion bossbar
        Component title = plugin.parseMessage(
            "<green><bold>✓ QUEST COMPLETED! <gray>" + quest.getName() + " <gray>- <yellow>Use /quests to claim!"
        );
        
        BossBar bossBar = BossBar.bossBar(title, 1.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        
        // Show bossbar to player
        player.showBossBar(bossBar);
        activeBossBars.put(playerId, bossBar);
        
        // Schedule hide task (hide after 8 seconds for completion)
        BukkitRunnable hideTask = new BukkitRunnable() {
            @Override
            public void run() {
                hideBossBar(player);
            }
        };
        hideTask.runTaskLater(plugin, 160L); // 8 seconds
        hideTaskMap.put(playerId, hideTask);
    }
    
    public void hideBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel hide task
        BukkitRunnable hideTask = hideTaskMap.remove(playerId);
        if (hideTask != null) {
            hideTask.cancel();
        }
        
        // Hide bossbar
        BossBar bossBar = activeBossBars.remove(playerId);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }
    
    public void hideAllBossBars(Player player) {
        hideBossBar(player);
    }
    
    private BossBar.Color getColorForProgress(int percentage) {
        if (percentage >= 100) return BossBar.Color.GREEN;
        if (percentage >= 75) return BossBar.Color.BLUE;
        if (percentage >= 50) return BossBar.Color.YELLOW;
        if (percentage >= 25) return BossBar.Color.PINK;
        return BossBar.Color.RED;
    }
    
    public void cleanup() {
        // Cancel all hide tasks
        for (BukkitRunnable task : hideTaskMap.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        hideTaskMap.clear();
        
        // Hide all boss bars
        for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.hideBossBar(entry.getValue());
            }
        }
        activeBossBars.clear();
    }
}
