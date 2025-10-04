package com.ryzz3nn.woidzquests.commands;

import com.ryzz3nn.woidzquests.WoidZQuests;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WoidZQuestsCommand implements CommandExecutor, TabCompleter {
    
    private final WoidZQuests plugin;
    
    public WoidZQuestsCommand(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("woidzquests.admin")) {
            sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getNoPermission()));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getReloadSuccess()));
            }
            case "qp", "questpoints" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Usage: /woidzquests qp <give|take|set|check> <player> [amount]"));
                    return true;
                }
                handleQuestPointsCommand(sender, args);
            }
            case "reroll" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Usage: /woidzquests reroll <daily|weekly> <player>"));
                    return true;
                }
                handleRerollCommand(sender, args);
            }
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(plugin.parseMessage(plugin.getMessagesManager().getMessage("unknown-command")));
                sendHelp(sender);
            }
        }
        
        return true;
    }
    
    private void handleQuestPointsCommand(CommandSender sender, String[] args) {
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "give" -> {
                if (args.length < 4) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Usage: /woidzquests qp give <player> <amount>"));
                    return;
                }
                
                String playerName = args[2];
                Player target = Bukkit.getPlayer(playerName);
                
                if (target == null) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Player not found: <red>" + playerName + "<reset>"));
                    return;
                }
                
                try {
                    int amount = Integer.parseInt(args[3]);
                    String command = "qp give " + target.getName() + " " + amount;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    
                    sender.sendMessage(plugin.parseMessage("<gray>[<green><bold>✓</bold></green><gray>]<reset> Gave <light_purple>" + amount + " Quest Points<reset> to <yellow>" + target.getName() + "<reset>"));
                    target.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>★</bold></light_purple><gray>]<reset> You received <light_purple>" + amount + " Quest Points<reset> from an admin!"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Invalid amount: <red>" + args[3] + "<reset>"));
                }
            }
            case "take" -> {
                if (args.length < 4) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Usage: /woidzquests qp take <player> <amount>"));
                    return;
                }
                
                String playerName = args[2];
                Player target = Bukkit.getPlayer(playerName);
                
                if (target == null) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Player not found: <red>" + playerName + "<reset>"));
                    return;
                }
                
                try {
                    int amount = Integer.parseInt(args[3]);
                    String command = "qp take " + target.getName() + " " + amount;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    
                    sender.sendMessage(plugin.parseMessage("<gray>[<green><bold>✓</bold></green><gray>]<reset> Took <light_purple>" + amount + " Quest Points<reset> from <yellow>" + target.getName() + "<reset>"));
                    target.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>★</bold></light_purple><gray>]<reset> <light_purple>" + amount + " Quest Points<reset> were removed by an admin."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Invalid amount: <red>" + args[3] + "<reset>"));
                }
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Usage: /woidzquests qp set <player> <amount>"));
                    return;
                }
                
                String playerName = args[2];
                Player target = Bukkit.getPlayer(playerName);
                
                if (target == null) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Player not found: <red>" + playerName + "<reset>"));
                    return;
                }
                
                try {
                    int amount = Integer.parseInt(args[3]);
                    String command = "qp set " + target.getName() + " " + amount;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    
                    sender.sendMessage(plugin.parseMessage("<gray>[<green><bold>✓</bold></green><gray>]<reset> Set <yellow>" + target.getName() + "'s<reset> Quest Points to <light_purple>" + amount + "<reset>"));
                    target.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>★</bold></light_purple><gray>]<reset> Your Quest Points were set to <light_purple>" + amount + "<reset> by an admin."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Invalid amount: <red>" + args[3] + "<reset>"));
                }
            }
            case "check", "balance" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Usage: /woidzquests qp check <player>"));
                    return;
                }
                
                String playerName = args[2];
                Player target = Bukkit.getPlayer(playerName);
                
                if (target == null) {
                    sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Player not found: <red>" + playerName + "<reset>"));
                    return;
                }
                
                // This would require integrating with CoinsEngine API to get actual balance
                // For now, show a placeholder message
                sender.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>★</bold></light_purple><gray>]<reset> <yellow>" + target.getName() + "'s<reset> Quest Points: <light_purple>Use /qp balance " + target.getName() + "<reset>"));
            }
            default -> {
                sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Unknown action: <red>" + action + "<reset>"));
                sender.sendMessage(plugin.parseMessage("<gray>Available actions: <yellow>give, take, set, check<reset>"));
            }
        }
    }
    
    private void handleRerollCommand(CommandSender sender, String[] args) {
        String questType = args[1].toLowerCase();
        String playerName = args[2];
        
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Player not found: <red>" + playerName + "<reset>"));
            return;
        }
        
        switch (questType) {
            case "daily" -> {
                plugin.getDailyQuestManager().generateNewDailyQuestsForPlayer(target.getUniqueId());
                sender.sendMessage(plugin.parseMessage("<gray>[<green><bold>✓</bold></green><gray>]<reset> Rerolled daily quests for <yellow>" + target.getName() + "<reset>"));
                target.sendMessage(plugin.parseMessage("<gray>[<aqua><bold>⟲</bold></aqua><gray>]<reset> Your daily quests have been rerolled by an admin!"));
            }
            case "weekly" -> {
                plugin.getWeeklyQuestManager().generateNewWeeklyQuestsForPlayer(target.getUniqueId());
                sender.sendMessage(plugin.parseMessage("<gray>[<green><bold>✓</bold></green><gray>]<reset> Rerolled weekly quests for <yellow>" + target.getName() + "<reset>"));
                target.sendMessage(plugin.parseMessage("<gray>[<light_purple><bold>⟲</bold></light_purple><gray>]<reset> Your weekly quests have been rerolled by an admin!"));
            }
            default -> {
                sender.sendMessage(plugin.parseMessage("<gray>[<red><bold>!</bold></red><gray>]<reset> Invalid quest type: <red>" + questType + "<reset>"));
                sender.sendMessage(plugin.parseMessage("<gray>Available types: <yellow>daily, weekly<reset>"));
            }
        }
    }
    
    private void sendHelp(CommandSender sender) {
        List<String> helpMessages = plugin.getMessagesManager().getMessageList("commands.help");
        if (sender.hasPermission("woidzquests.admin")) {
            helpMessages.addAll(plugin.getMessagesManager().getMessageList("commands.admin-help"));
        }
        
        for (String message : helpMessages) {
            sender.sendMessage(plugin.parseMessage(message));
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("woidzquests.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("reload", "help", "qp", "questpoints", "reroll"));
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("qp") || args[0].equalsIgnoreCase("questpoints"))) {
            List<String> completions = new ArrayList<>(Arrays.asList("give", "take", "set", "check", "balance"));
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("reroll")) {
            List<String> completions = new ArrayList<>(Arrays.asList("daily", "weekly"));
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        }
        
        if (args.length == 3 && (args[0].equalsIgnoreCase("qp") || args[0].equalsIgnoreCase("questpoints"))) {
            // Return online player names
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                .toList();
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("reroll")) {
            // Return online player names for reroll command
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                .toList();
        }
        
        return new ArrayList<>();
    }
}
