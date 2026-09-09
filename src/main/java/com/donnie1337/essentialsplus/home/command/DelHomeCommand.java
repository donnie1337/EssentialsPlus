package com.donnie1337.essentialsplus.home.command;

import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import com.donnie1337.essentialsplus.home.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DelHomeCommand implements CommandExecutor {
    private final HomeService service;
    private final ChatPlusBridge chatPlus = new ChatPlusBridge();

    public DelHomeCommand(HomeService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission("essentialsplus.delhome")) {
            message(player, "no-permission");
            return true;
        }
        if (!service.enabled()) {
            message(player, "homes-disabled");
            return true;
        }
        if (args.length != 1) {
            message(player, "usage-delhome");
            return true;
        }
        try {
            if (!service.deleteHome(player, args[0])) {
                message(player, "home-not-found", "home", args[0]);
                return true;
            }
            message(player, "home-deleted", "home", args[0].toLowerCase());
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
