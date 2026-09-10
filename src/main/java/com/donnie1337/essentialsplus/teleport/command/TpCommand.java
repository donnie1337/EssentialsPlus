package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.teleport.StaffTeleportService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TpCommand implements CommandExecutor {
    private final StaffTeleportService service;

    public TpCommand(StaffTeleportService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("essentialsplus.teleport")) {
            send(sender, "no-permission");
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                send(sender, "player-only");
                return true;
            }
            Player target = service.findPlayer(args[0]);
            if (target == null) {
                send(sender, "player-not-found");
                return true;
            }
            if (!service.teleport(player, target)) {
                send(sender, "teleport-failed");
                return true;
            }
            send(sender, "teleported-to", "player", target.getName());
            return true;
        }

        if (args.length == 2) {
            Player player = service.findPlayer(args[0]);
            Player target = service.findPlayer(args[1]);
            if (player == null || target == null) {
                send(sender, "player-not-found");
                return true;
            }
            if (!service.teleport(player, target, player)) {
                send(sender, "teleport-failed");
                return true;
            }
            send(sender, "player-teleported-to", "player", player.getName(), "target", target.getName());
            return true;
        }

        send(sender, "usage-tp");
        return true;
    }

    private void send(CommandSender sender, String key, String... replacements) {
        String raw = service.plugin().getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        sender.sendMessage(raw.replace('&', '§'));
    }
}
