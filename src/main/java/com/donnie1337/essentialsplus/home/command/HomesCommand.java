package com.donnie1337.essentialsplus.home.command;

import com.donnie1337.essentialsplus.home.Home;
import com.donnie1337.essentialsplus.home.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class HomesCommand implements CommandExecutor {
    private final HomeService service;
    public HomesCommand(HomeService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando so pode ser usado por jogadores."); return true; }
        if (!player.hasPermission("essentialsplus.homes")) { player.sendMessage("§cVoce nao tem permissao para isso."); return true; }
        if (!service.enabled()) { player.sendMessage("§cO sistema de Homes esta desativado."); return true; }
        if (args.length != 0) { player.sendMessage("§cUso: /homes"); return true; }
        Map<String, Home> homes = service.homes(player);
        if (homes.isEmpty()) { player.sendMessage("§eVoce ainda nao possui nenhuma home."); return true; }
        player.sendMessage("§6§lSuas homes: §f" + String.join("§7, §f", homes.keySet()));
        return true;
    }
}
