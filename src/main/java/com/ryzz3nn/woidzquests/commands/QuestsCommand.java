package com.ryzz3nn.woidzquests.commands;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.ryzz3nn.woidzquests.gui.QuestMainGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class QuestsCommand implements CommandExecutor, TabCompleter {
    
    private final WoidZQuests plugin;
    
    public QuestsCommand(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
