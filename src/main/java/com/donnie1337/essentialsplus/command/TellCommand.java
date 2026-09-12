package com.donnie1337.essentialsplus.command;

import com.donnie1337.essentialsplus.chat.TellListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TellCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TellListener.message("messages.tell.player-only"));
            return true;
        }
        return TellListener.handleTellCommand(player, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("essentialsplus.tell")) {
            return List.of();
        }

        final String input = args.length == 0 || args[args.length - 1] == null
                ? ""
                : args[args.length - 1].toLowerCase(Locale.ROOT);

        final List<String> suggestions = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            final String name = online.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(input)) suggestions.add(name);
        }

        suggestions.sort(Comparator.naturalOrder());
        return suggestions;
    }
}
