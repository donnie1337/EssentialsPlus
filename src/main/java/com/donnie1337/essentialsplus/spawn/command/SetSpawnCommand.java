package com.donnie1337.essentialsplus.spawn.command;

import com.donnie1337.essentialsplus.spawn.SpawnService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetSpawnCommand implements CommandExecutor {

    private final SpawnService spawnService;

    public SetSpawnCommand(SpawnService spawnService) {
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando precisa ser usado por um jogador.");
            return true;
        }

        try {
            spawnService.setSpawn(player.getLocation());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6&lSPAWN &8• &aSpawn definido na sua localização atual."));
        } catch (IllegalStateException exception) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6&lSPAWN &8• &cNão foi possível salvar o spawn. Verifique o console."));
        }
        return true;
    }
}
