package com.ryzz3nn.woidzquests.commands;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.gui.QuestMainGUI;
import com.ryzz3nn.woidzquests.models.DailyQuest;
import com.ryzz3nn.woidzquests.models.WeeklyQuest;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestsCommand implements CommandExecutor, TabCompleter {
    
    private final WoidZQuests plugin;
    
    public QuestsCommand(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Handle reset subcommand
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            return handleResetCommand(sender, args);
        }
        
        // Default behavior: open quest GUI
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("player-only")));
            return true;
        }
        
        if (!player.hasPermission("woidzquests.use")) {
            player.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getNoPermission()));
            return true;
        }
        
        // Open quest GUI
        QuestMainGUI gui = new QuestMainGUI(plugin, player);
        gui.open();
        
        return true;
    }
    
    private boolean handleResetCommand(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("woidzquests.admin.reset")) {
            sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getNoPermission()));
            return true;
        }
        
        // Usage: /quests reset <player> <daily|weekly> <all|slot>
        if (args.length < 4) {
            sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Usage: <light_purple>/quests reset <player> <daily|weekly> <all|slot>"));
            return true;
        }
        
        String targetPlayerName = args[1];
        String questType = args[2].toLowerCase();
        String slotOrAll = args[3].toLowerCase();
        
        // Get target player
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.player-not-found")
                .replace("{player}", targetPlayerName)));
            return true;
        }
        
        // Validate quest type
        if (!questType.equals("daily") && !questType.equals("weekly")) {
            sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Quest type must be <light_purple>daily<reset> or <light_purple>weekly<reset>."));
            return true;
        }
        
        // Handle reset
        if (slotOrAll.equals("all")) {
            return handleResetAll(sender, targetPlayer, questType);
        } else {
            return handleResetSlot(sender, targetPlayer, questType, slotOrAll);
        }
    }
    
    private boolean handleResetAll(CommandSender sender, Player targetPlayer, String questType) {
        if (questType.equals("daily")) {
            // Reset all daily quests
            plugin.getDailyQuestManager().generateNewDailyQuestsForPlayer(targetPlayer.getUniqueId());
            
            sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.all-success")
                .replace("{player}", targetPlayer.getName())
                .replace("{type}", "daily")));
            
            if (targetPlayer.isOnline()) {
                targetPlayer.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.all-notification")
                    .replace("{type}", "daily")));
            }
        } else {
            // Reset all weekly quests
            plugin.getWeeklyQuestManager().generateNewWeeklyQuestsForPlayer(targetPlayer.getUniqueId());
            
            sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.all-success")
                .replace("{player}", targetPlayer.getName())
                .replace("{type}", "weekly")));
            
            if (targetPlayer.isOnline()) {
                targetPlayer.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.all-notification")
                    .replace("{type}", "weekly")));
            }
        }
        
        return true;
    }
    
    private boolean handleResetSlot(CommandSender sender, Player targetPlayer, String questType, String slotStr) {
        // Parse slot number
        int slot;
        try {
            slot = Integer.parseInt(slotStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Slot must be a number or <light_purple>all<reset>."));
            return true;
        }
        
        if (questType.equals("daily")) {
            // Reset specific daily quest slot
            List<DailyQuest> dailyQuests = plugin.getDailyQuestManager().getPlayerDailyQuests(targetPlayer.getUniqueId());
            
            if (slot < 1 || slot > dailyQuests.size()) {
                sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.invalid-slot")
                    .replace("{slot}", String.valueOf(slot))
                    .replace("{max}", String.valueOf(dailyQuests.size()))));
                return true;
            }
            
            // Reset the specific quest (1-indexed for user, 0-indexed internally)
            plugin.getDailyQuestManager().resetQuestSlot(targetPlayer.getUniqueId(), slot - 1);
            
            sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.slot-success")
                .replace("{player}", targetPlayer.getName())
                .replace("{type}", "daily")
                .replace("{slot}", String.valueOf(slot))));
            
            if (targetPlayer.isOnline()) {
                targetPlayer.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.slot-notification")
                    .replace("{type}", "daily")
                    .replace("{slot}", String.valueOf(slot))));
            }
        } else {
            // Reset specific weekly quest slot
            List<WeeklyQuest> weeklyQuests = plugin.getWeeklyQuestManager().getPlayerWeeklyQuests(targetPlayer.getUniqueId());
            
            if (slot < 1 || slot > weeklyQuests.size()) {
                sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.invalid-slot")
                    .replace("{slot}", String.valueOf(slot))
                    .replace("{max}", String.valueOf(weeklyQuests.size()))));
                return true;
            }
            
            // Reset the specific quest (1-indexed for user, 0-indexed internally)
            plugin.getWeeklyQuestManager().resetQuestSlot(targetPlayer.getUniqueId(), slot - 1);
            
            sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.slot-success")
                .replace("{player}", targetPlayer.getName())
                .replace("{type}", "weekly")
                .replace("{slot}", String.valueOf(slot))));
            
            if (targetPlayer.isOnline()) {
                targetPlayer.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("commands.reset.slot-notification")
                    .replace("{type}", "weekly")
                    .replace("{slot}", String.valueOf(slot))));
            }
        }
        
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("woidzquests.admin.reset")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument: reset
            completions.add("reset");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            // Second argument: player name
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
            // Third argument: quest type
            completions.add("daily");
            completions.add("weekly");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("reset")) {
            // Fourth argument: all or slot number
            completions.add("all");
            completions.add("1");
            completions.add("2");
            completions.add("3");
            completions.add("4");
            completions.add("5");
        }
        
        return completions;
    }
}
