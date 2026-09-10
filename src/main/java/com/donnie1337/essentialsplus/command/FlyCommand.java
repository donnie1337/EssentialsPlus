package com.donnie1337.essentialsplus.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FlyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (!player.hasPermission("essentialsplus.fly")) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        boolean enabled = !player.getAllowFlight();
        player.setAllowFlight(enabled);
        player.setFlying(enabled);

        player.sendMessage(enabled
                ? "§7§lᴜᴛɪʟɪᴅᴀᴅᴇs §8• §r§aModo voo ativado."
                : "§7§lᴜᴛɪʟɪᴅᴀᴅᴇs §8• §r§cModo voo desativado.");
        return true;
    }
}
