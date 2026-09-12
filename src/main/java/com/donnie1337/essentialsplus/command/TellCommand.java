package com.donnie1337.essentialsplus.command;

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

/**
 * Registers /tell with Bukkit so the command participates in tab completion.
 * The actual /tell execution is handled by TellListener through command preprocessing.
 */
public final class TellCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TellListener handles the command through PlayerCommandPreprocessEvent.
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("essentialsplus.tell")) {
            return List.of();
        }

        if (args.length != 1) {
            return List.of();
        }

        String input = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            String name = online.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(input)) {
                suggestions.add(name);
            }
        }

        suggestions.sort(Comparator.naturalOrder());
        return suggestions;
    }
}
