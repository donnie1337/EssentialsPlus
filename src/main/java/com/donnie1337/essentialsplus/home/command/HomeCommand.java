package com.donnie1337.essentialsplus.home.command;

import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import com.donnie1337.essentialsplus.home.Home;
import com.donnie1337.essentialsplus.home.HomeService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HomeCommand implements CommandExecutor {
    private final HomeService service;
    private final ChatPlusBridge chatPlus = new ChatPlusBridge();

    public HomeCommand(HomeService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission("essentialsplus.home")) {
            message(player, "geral.sem-permissao");
            return true;
        }
        if (!service.enabled()) {
            message(player, "home.sistema-desativado");
            return true;
        }
        if (args.length != 1) {
            message(player, "home.uso.home");
            return true;
        }

        try {
            Home home = service.getHome(player, args[0]);
            if (home == null) {
                message(player, "geral.jogador-nao-encontrado");
                return true;
            }

            Location location = home.location();
            if (location.getWorld() == null) {
                message(player, "home.mundo-indisponivel");
                return true;
            }
            if (!valid(location)) {
                message(player, "home.localizacao-invalida");
                return true;
            }
            if (!player.isOnline()) return true;

            if (!player.teleport(location)) {
                message(player, "home.teleporte-falhou");
                return true;
            }
            message(player, "home.teleportado", "home", home.name());
        } catch (IllegalArgumentException exception) {
            message(player, "home.nome-invalido");
        }
        return true;
    }

    private boolean valid(Location location) {
        return Double.isFinite(location.getX())
                && Double.isFinite(location.getY())
                && Double.isFinite(location.getZ())
                && Float.isFinite(location.getYaw())
                && Float.isFinite(location.getPitch());
    }

    private void message(Player player, String path, String... replacements) {
        String prefix = service.plugin().getConfig().getString("messages.home.prefix", "&a&lʜᴏᴍᴇ &8• &r");
        String raw = service.plugin().getConfig().getString("messages." + path, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        String colored = (prefix + raw).replace('&', '§');
        if (!chatPlus.sendDirect(player, colored)) {
            player.sendMessage(colored);
        }
    }
}
