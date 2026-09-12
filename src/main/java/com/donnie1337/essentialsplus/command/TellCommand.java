package com.donnie1337.essentialsplus.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Registers /tell directly in Paper's Brigadier command tree.
 * The actual /tell execution is handled by TellListener through command preprocessing.
 */
public final class TellCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        // TellListener handles the command through PlayerCommandPreprocessEvent.
    }

    @Override
    public @Nullable String permission() {
        return "essentialsplus.tell";
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        final CommandSender sender = source.getSender();
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

        suggestions.sort(Comparator.naturalOrder());
        return suggestions;
    }
}
