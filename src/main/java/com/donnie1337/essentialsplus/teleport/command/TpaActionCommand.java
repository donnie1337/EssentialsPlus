package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.teleport.TeleportService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TpaActionCommand implements CommandExecutor {
    private static final String PERMISSION = "essentialsplus.tpa";
    private final TeleportService service;

    public TpaActionCommand(TeleportService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cVocê não tem permissão para isso.");
            return true;
        }
        if (args.length != 1) return true;
        try {
            int token = Integer.parseInt(args[0]);
            service.handleButton(player, token);
        } catch (NumberFormatException ignored) {
            // Token inválido: não executa nenhuma ação.
        }
        return true;
    }
}
