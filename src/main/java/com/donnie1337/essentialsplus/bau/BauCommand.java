package com.donnie1337.essentialsplus.bau;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BauCommand implements CommandExecutor {
    private final BauService bauService;

    public BauCommand(BauService bauService) {
        this.bauService = bauService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (!player.hasPermission("essentialsplus.bau")) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        player.openInventory(bauService.createInventory(player));
        return true;
    }
}
