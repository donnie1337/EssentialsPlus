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
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§e&lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e&lᴄʜᴀᴛ §8• §rUse /r <mensagem>.");
            return true;
        }

        Player target = TellListener.getLastTarget(player);
        if (target == null) {
            player.sendMessage("§e&lᴄʜᴀᴛ §8• §cVocê não possui nenhuma conversa privada recente.");
            return true;
        }

        if (!TellListener.canReceiveTell(target)) {
            player.sendMessage("§e&lᴄʜᴀᴛ §8• §cO jogador desativou as mensagens privadas.");
            return true;
        }

        TellListener.sendPrivateMessage(player, target, String.join(" ", args));
        return true;
    }
}
