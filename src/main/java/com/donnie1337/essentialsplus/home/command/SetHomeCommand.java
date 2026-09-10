package com.donnie1337.essentialsplus.home.command;

import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import com.donnie1337.essentialsplus.home.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetHomeCommand implements CommandExecutor {
    private final HomeService service;
    private final ChatPlusBridge chatPlus = new ChatPlusBridge();

    public SetHomeCommand(HomeService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission("essentialsplus.sethome")) {
            message(player, "no-permission");
            return true;
        }
        if (!service.enabled()) {
            message(player, "homes-disabled");
            return true;
        }
        if (args.length != 1) {
            if (args.length > 1) {
                message(player, "invalid-home-name");
            } else {
                message(player, "usage-sethome");
            }
            return true;
        }
        try {
            if (!args[0].matches("[a-zA-Z0-9_-]{1,32}")) {
                message(player, "invalid-home-name");
                return true;
            }

            if (!service.setHome(player, args[0])) {
                message(player, "home-limit", "limit", String.valueOf(service.getHomeLimit(player)));
                return true;
            }
            message(player, "home-saved", "home", args[0].toLowerCase());
        } catch (IllegalArgumentException exception) {
            message(player, "invalid-home-name");
        }
        return true;
    }

    private void message(Player player, String key, String... replacements) {
        String raw = service.plugin().getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        String colored = raw.replace('&', '§');
        if (!chatPlus.sendSystemMessage(player, colored)) {
            String prefix = service.plugin().getConfig().getString("messages.prefix", "").replace('&', '§');
            player.sendMessage(prefix + colored);
        }
    }
}
