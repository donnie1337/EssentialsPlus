package com.donnie1337.essentialsplus.home.command;

import com.donnie1337.essentialsplus.home.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DelHomeCommand implements CommandExecutor {
    private final HomeService service;
    public DelHomeCommand(HomeService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando so pode ser usado por jogadores."); return true; }
        if (!player.hasPermission("essentialsplus.delhome")) { player.sendMessage("§cVoce nao tem permissao para isso."); return true; }
        if (args.length != 1) { player.sendMessage("§cUso: /delhome <nome>"); return true; }
        try {
            if (!service.deleteHome(player, args[0])) { player.sendMessage("§cHome §f" + args[0] + " §cnao encontrada."); return true; }
            player.sendMessage("§aHome §f" + args[0] + " §aremovida com sucesso.");
        } catch (IllegalArgumentException exception) {
            player.sendMessage("§cNome de home invalido.");
        }
        return true;
    }
}
