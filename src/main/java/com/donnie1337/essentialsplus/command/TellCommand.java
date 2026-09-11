package com.donnie1337.essentialsplus.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public final class TellCommand implements CommandExecutor {
    private static final String PERMISSION = "essentialsplus.tell";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§e&lᴄʜᴀᴛ §8• §rUse /tell <jogador> <mensagem>.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§e&lᴄʜᴀᴛ §8• §cJogador não encontrado ou offline.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§e&lᴄʜᴀᴛ §8• §cVocê não pode enviar uma mensagem para si mesmo.");
            return true;
        }

        if (!receivesTell(target)) {
            player.sendMessage("§e&lᴄʜᴀᴛ §8• §cO jogador desativou as mensagens privadas.");
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String formattedTo = "§e&lᴄʜᴀᴛ §8• §7Você → §f" + target.getName() + "§8: §r" + message;
        String formattedFrom = "§e&lᴄʜᴀᴛ §8• §7" + player.getName() + " → §fVocê§8: §r" + message;

        player.sendMessage(formattedTo);
        target.sendMessage(formattedFrom);
        return true;
    }

    private boolean receivesTell(Player player) {
        Plugin utilidades = Bukkit.getPluginManager().getPlugin("UtilidadesPlus");
        if (utilidades == null || !utilidades.isEnabled()) return true;

        try {
            Method method = utilidades.getClass().getMethod("receivesTell", Player.class);
            Object result = method.invoke(utilidades, player);
            return result instanceof Boolean value ? value : true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }
}
