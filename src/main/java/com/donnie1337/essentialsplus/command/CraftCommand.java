package com.donnie1337.essentialsplus.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CraftCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (!player.hasPermission("essentialsplus.craft")) {
            player.sendMessage("Comando não encontrado.");
            return true;
        }

        player.openWorkbench(player.getLocation(), true);
        return true;
    }
}
