package com.donnie1337.essentialsplus.command;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class GameModeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Uso: /gamemode <0|1|2|3>");
            return true;
        }

        GameMode gameMode = parseGameMode(args[0]);
        if (gameMode == null) {
            player.sendMessage(ChatColor.RED + "Modo inválido. Use 0, 1, 2 ou 3.");
            return true;
        }

        player.setGameMode(gameMode);
        player.sendMessage(ChatColor.GREEN + "Modo de jogo alterado para " + ChatColor.YELLOW
                + displayName(gameMode) + ChatColor.GREEN + ".");
        return true;
    }

    private GameMode parseGameMode(String value) {
        if (value == null) return null;

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "0", "survival", "sobrevivencia", "sobrevivência" -> GameMode.SURVIVAL;
            case "1", "creative", "criativo" -> GameMode.CREATIVE;
            case "2", "adventure", "aventura" -> GameMode.ADVENTURE;
            case "3", "spectator", "espectador" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private String displayName(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> "Survival";
            case CREATIVE -> "Creative";
            case ADVENTURE -> "Adventure";
            case SPECTATOR -> "Spectator";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();

        String start = args[0].toLowerCase(Locale.ROOT);
        return List.of("0", "1", "2", "3", "survival", "creative", "adventure", "spectator").stream()
                .filter(option -> option.startsWith(start))
                .toList();
    }
}
