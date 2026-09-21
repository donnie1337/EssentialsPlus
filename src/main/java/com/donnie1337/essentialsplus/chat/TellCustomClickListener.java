package com.donnie1337.essentialsplus.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Comando interno acionado pelo botão de cancelamento do /tell. */
public final class TellCustomClickListener implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        TellListener.CancelResult result = TellListener.cancelPendingTell(player);
        switch (result) {
            case CANCELLED -> player.sendMessage(TellListener.message("messages.tell.cancelled"));
            case ALREADY_SENT -> player.sendMessage(TellListener.message("messages.tell.already-sent"));
            case ALREADY_CANCELLED -> player.sendMessage(TellListener.message("messages.tell.already-cancelled"));
        }
        return true;
    }
}
