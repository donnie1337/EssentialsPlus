package com.donnie1337.essentialsplus.home.command;

import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import com.donnie1337.essentialsplus.home.HomeGui;
import com.donnie1337.essentialsplus.home.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HomesCommand implements CommandExecutor {
    private final HomeService service;
    private final HomeGui gui;
    private final ChatPlusBridge chatPlus = new ChatPlusBridge();

    public HomesCommand(HomeService service, HomeGui gui) {
        this.service = service;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission("essentialsplus.homes")) {
            message(player, "no-permission");
            return true;
        }
        if (!service.enabled()) {
            message(player, "homes-disabled");
            return true;
        }
        if (args.length != 0) {
            message(player, "usage-homes");
            return true;
        }

        gui.openIntro(player);
        return true;
    }

    private void message(Player player, String key) {
        String colored = service.plugin().getConfig().getString("messages." + key, "").replace('&', '§');
        if (!chatPlus.sendSystemMessage(player, colored)) {
            String prefix = service.plugin().getConfig().getString("messages.prefix", "").replace('&', '§');
            player.sendMessage(prefix + colored);
        }
    }
}
