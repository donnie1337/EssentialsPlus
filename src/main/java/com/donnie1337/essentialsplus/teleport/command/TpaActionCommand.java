package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.teleport.TeleportService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Processa tokens efêmeros enviados pelos botões de TPA no chat. */
public final class TpaActionCommand implements CommandExecutor {
    private final TeleportService teleportService;

    public TpaActionCommand(TeleportService teleportService) {
        this.teleportService = teleportService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) return true;

        try {
            teleportService.handleButton(player, Integer.parseInt(args[0]));
        } catch (NumberFormatException ignored) {
            // Token inválido: não executa nenhuma ação.
        }
        return true;
    }
}
