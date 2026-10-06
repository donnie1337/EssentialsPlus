package com.donnie1337.essentialsplus.spawn.command;

import com.donnie1337.essentialsplus.spawn.SpawnService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpawnCommand implements CommandExecutor {

    private final SpawnService spawnService;

    public SpawnCommand(SpawnService spawnService) {
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando precisa ser usado por um jogador.");
            return true;
        }

        Location spawn = spawnService.getSpawn().orElse(null);
        if (spawn == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6&lSPAWN &8• &cO spawn ainda não foi definido."));
            return true;
        }

        boolean success = player.teleport(spawn);
        if (!success) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6&lSPAWN &8• &cNão foi possível teleportar você para o spawn."));
            return true;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6&lSPAWN &8• &aTeleportado para o spawn."));
        return true;
    }
}
