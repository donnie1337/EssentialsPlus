package com.donnie1337.essentialsplus.spawn.command;

import com.donnie1337.essentialsplus.EssentialsPlus;
import com.donnie1337.essentialsplus.spawn.SpawnService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpawnCommand implements CommandExecutor {

    private final EssentialsPlus plugin;
    private final SpawnService spawnService;

    public SpawnCommand(EssentialsPlus plugin, SpawnService spawnService) {
        this.plugin = plugin;
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("jogador-apenas",
                    "Este comando precisa ser usado por um jogador."));
            return true;
        }

        Location spawn = spawnService.getSpawn().orElse(null);
        if (spawn == null) {
            player.sendMessage(message("nao-definido",
                    "&cO spawn ainda não foi definido."));
            return true;
        }

        try {
            if (!spawn.getChunk().isLoaded()) {
                spawn.getChunk().load(true);
            }
        } catch (RuntimeException exception) {
            player.sendMessage(message("carregamento-falhou",
                    "&cNão foi possível carregar o local do spawn."));
            return true;
        }

        boolean success;
        try {
            success = player.teleport(spawn);
        } catch (RuntimeException exception) {
            success = false;
        }

        if (!success) {
            player.sendMessage(message("teleporte-falhou",
                    "&cNão foi possível teleportar você para o spawn."));
            return true;
        }

        player.sendMessage(message("teleportado",
                "&aTeleportado para o spawn."));
        return true;
    }

    private String message(String key, String fallback) {
        String prefix = plugin.getConfig().getString(
                "messages.spawn.prefix",
                "&6&lꜱᴘᴀᴡɴ &8• &r"
        );
        String body = plugin.getConfig().getString("messages.spawn." + key, fallback);
        return ChatColor.translateAlternateColorCodes('&', prefix + body);
    }
}
