package com.donnie1337.essentialsplus.spawn.command;

import com.donnie1337.essentialsplus.EssentialsPlus;
import com.donnie1337.essentialsplus.spawn.SpawnService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetSpawnCommand implements CommandExecutor {

    private final EssentialsPlus plugin;
    private final SpawnService spawnService;

    public SetSpawnCommand(EssentialsPlus plugin, SpawnService spawnService) {
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

        try {
            spawnService.setSpawn(player.getLocation());
            player.sendMessage(message("definido",
                    "&aSpawn definido na sua localização atual."));
        } catch (IllegalStateException exception) {
            player.sendMessage(message("salvar-falhou",
                    "&cNão foi possível salvar o spawn. Verifique o console."));
        }
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
