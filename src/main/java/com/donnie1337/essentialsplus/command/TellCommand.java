package com.donnie1337.essentialsplus.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Registers /tell using the standard Spigot command API.
 * TellListener handles the actual command through PlayerCommandPreprocessEvent.
 */
public final class TellCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TellListener handles /tell through PlayerCommandPreprocessEvent.
        return true;
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
            if (online.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            final String name = online.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(input)) {
                suggestions.add(name);
            }
        }

        suggestions.sort(String.CASE_INSENSITIVE_ORDER);
        return suggestions;
    }
}
