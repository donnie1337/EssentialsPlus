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
            message(player, "geral.sem-permissao");
            return true;
        }
        if (!service.enabled()) {
            message(player, "home.sistema-desativado");
            return true;
        }
        if (args.length != 1) {
            message(player, "home.uso.delhome");
            return true;
        }
        try {
            if (!service.deleteHome(player, args[0])) {
                message(player, "home.nao-encontrada", "home", args[0]);
                return true;
            }
            message(player, "home.removida", "home", args[0].toLowerCase());
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
        if (!chatPlus.sendDirect(player, colored)) {
            player.sendMessage(colored);
        }
    }
}
