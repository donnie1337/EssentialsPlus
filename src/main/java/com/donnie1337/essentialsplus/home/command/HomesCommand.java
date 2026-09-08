package com.donnie1337.essentialsplus.home.command;

import com.donnie1337.essentialsplus.home.HomeGui;
import com.donnie1337.essentialsplus.home.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HomesCommand implements CommandExecutor {
    private final HomeService service;
    private final HomeGui gui;

    public HomesCommand(HomeService service, HomeGui gui) {
        this.service = service;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando so pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission("essentialsplus.homes")) {
            player.sendMessage("§cVoce nao tem permissao para isso.");
            return true;
        }
        if (!service.enabled()) {
            player.sendMessage("§cO sistema de Homes esta desativado.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage("§cUso: /homes");
            return true;
        }

        gui.openIntro(player);
        return true;
    }
}
