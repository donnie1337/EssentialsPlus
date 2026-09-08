package com.donnie1337.essentialsplus.home.command;

import com.donnie1337.essentialsplus.home.Home;
import com.donnie1337.essentialsplus.home.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HomeCommand implements CommandExecutor {
    private final HomeService service;
    public HomeCommand(HomeService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando so pode ser usado por jogadores."); return true; }
        if (!player.hasPermission("essentialsplus.home")) { player.sendMessage("§cVoce nao tem permissao para isso."); return true; }
        if (!service.enabled()) { player.sendMessage("§cO sistema de Homes esta desativado."); return true; }
        if (args.length != 1) { player.sendMessage("§cUso: /home <nome>"); return true; }
        try {
            Home home = service.getHome(player, args[0]);
            if (home == null) { player.sendMessage("§cHome §f" + args[0] + " §cnao encontrada."); return true; }
            if (home.location().getWorld() == null) { player.sendMessage("§cNao foi possivel carregar o mundo desta home."); return true; }
            player.teleport(home.location());
            player.sendMessage("§aTeleportado para a home §f" + home.name() + "§a.");
        } catch (IllegalArgumentException exception) {
            player.sendMessage("§cNome de home invalido. Use apenas letras, numeros, _ ou - (ate 32 caracteres).");
        }
        return true;
    }
}
