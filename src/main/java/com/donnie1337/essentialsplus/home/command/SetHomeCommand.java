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
            message(player, "geral.sem-permissao");
            return true;
        }
        if (!service.enabled()) {
            message(player, "home.sistema-desativado");
            return true;
        }
        if (args.length != 1) {
            if (args.length > 1) {
                message(player, "home.nome-invalido");
            } else {
                message(player, "home.uso.sethome");
            }
            return true;
        }
        try {
            if (!args[0].matches("[a-zA-Z0-9_-]{1,32}")) {
                message(player, "home.nome-invalido");
                return true;
            }

            if (!service.setHome(player, args[0])) {
                message(player, "home.limite-atingido", "limit", String.valueOf(service.getHomeLimit(player)));
                return true;
            }
            message(player, "home.salva", "home", args[0].toLowerCase());
        } catch (IllegalArgumentException exception) {
            message(player, "home.nome-invalido");
        }
        return true;
    }

    private void message(Player player, String path, String... replacements) {
        String prefix = service.plugin().getConfig().getString("messages.home.prefix", "&a&lʜᴏᴍᴇ &8• &r");
        String raw = service.plugin().getConfig().getString("messages." + path, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        String colored = (prefix + raw).replace('&', '§');
        if (!chatPlus.sendSystemMessage(player, colored)) {
            player.sendMessage(colored);
        }
    }
}
