package com.donnie1337.essentialsplus.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public final class TellListener implements Listener {
    private static final String PERMISSION = "essentialsplus.tell";

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.length() < 6 || message.charAt(0) != '/') return;

        String[] args = message.substring(1).trim().split("\\s+");
        if (args.length < 1) return;

        String command = args[0].toLowerCase(Locale.ROOT);
        if (!command.equals("tell") && !command.equals("msg") && !command.equals("w") && !command.equals("whisper")) return;

        Player sender = event.getPlayer();
        event.setCancelled(true);

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§e&lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§e&lᴄʜᴀᴛ §8• §rUse /tell <jogador> <mensagem>.");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§e&lᴄʜᴀᴛ §8• §cJogador não encontrado ou offline.");
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage("§e&lᴄʜᴀᴛ §8• §cVocê não pode enviar uma mensagem para si mesmo.");
            return;
        }

        if (!receivesTell(target)) {
            sender.sendMessage("§e&lᴄʜᴀᴛ §8• §cO jogador desativou as mensagens privadas.");
            return;
        }

        String privateMessage = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        sender.sendMessage("§e&lᴄʜᴀᴛ §8• §7Você → §f" + target.getName() + "§8: §r" + privateMessage);
        target.sendMessage("§e&lᴄʜᴀᴛ §8• §7" + sender.getName() + " → §fVocê§8: §r" + privateMessage);
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
