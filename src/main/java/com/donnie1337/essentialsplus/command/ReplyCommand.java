package com.donnie1337.essentialsplus.command;

import com.donnie1337.essentialsplus.chat.TellListener;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class ReplyCommand implements CommandExecutor {
    private static final String PERMISSION = "essentialsplus.tell";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TellListener.message("messages.tell.player-only"));
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(TellListener.message("messages.tell.unknown-command"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(TellListener.message("messages.tell.reply-usage"));
            return true;
        }

        Player target = TellListener.getLastTarget(player);
        if (target == null) {
            player.sendMessage(TellListener.message("messages.tell.no-recent"));
            return true;
        }

        if (!TellListener.canReceiveTell(target)) {
            player.sendMessage(TellListener.message("messages.tell.target-disabled"));
            return true;
        }

        TellListener.sendPrivateMessage(player, target, String.join(" ", args));
        return true;
    }
}
