package com.donnie1337.essentialsplus.command;

import com.donnie1337.essentialsplus.flight.FlightService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FlyCommand implements CommandExecutor {
    private final FlightService flightService;

    public FlyCommand(FlightService flightService) {
        this.flightService = flightService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (!flightService.canFly(player)) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        boolean enabled = flightService.toggle(player);
        player.sendMessage(enabled
                ? "§a§lғʟʏ §8• §rModo de voo ativado."
                : "§c§lғʟʏ §8• §rModo de voo desativado.");
        return true;
    }
}
