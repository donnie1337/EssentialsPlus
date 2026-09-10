package com.donnie1337.essentialsplus.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class EcCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (!player.hasPermission("essentialsplus.ec")) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        Inventory enderChest = Bukkit.createInventory(player, 54, "§8Ender Chest");
        player.openInventory(enderChest);
        return true;
    }
}
