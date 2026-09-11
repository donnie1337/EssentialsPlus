package com.donnie1337.essentialsplus.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TellListener implements Listener {
    private static final String PERMISSION = "essentialsplus.tell";
    private static final ConcurrentHashMap<UUID, UUID> LAST_TARGETS = new ConcurrentHashMap<>();
    private static JavaPlugin plugin;

    public TellListener(JavaPlugin plugin) {
        TellListener.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') return;

        String[] args = message.substring(1).trim().split("\\s+");
        if (args.length < 1 || args[0].isBlank()) return;

        String command = args[0].toLowerCase(Locale.ROOT);
        if (command.equals("w") || command.equals("whisper") || command.equals("msg")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(message("messages.tell.unknown-command"));
            return;
        }

        if (!command.equals("tell")) return;

        Player sender = event.getPlayer();
        event.setCancelled(true);

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(message("messages.tell.unknown-command"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(message("messages.tell.usage"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(message("messages.tell.player-not-found"));
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(message("messages.tell.cannot-self"));
            return;
        }

        if (!receivesTell(target)) {
            sender.sendMessage(message("messages.tell.target-disabled"));
            return;
        }

        sendPrivateMessage(sender, target, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
    }

    public static Player getLastTarget(Player player) {
        UUID targetId = player == null ? null : LAST_TARGETS.get(player.getUniqueId());
        return targetId == null ? null : Bukkit.getPlayer(targetId);
    }

    public static void sendPrivateMessage(Player sender, Player target, String privateMessage) {
        LAST_TARGETS.put(sender.getUniqueId(), target.getUniqueId());
        LAST_TARGETS.put(target.getUniqueId(), sender.getUniqueId());
        sender.sendMessage(message("messages.tell.format-sender", "player", target.getName(), "message", privateMessage));
        target.sendMessage(message("messages.tell.format-target", "player", sender.getName(), "message", privateMessage));
    }

    public static boolean canReceiveTell(Player player) {
        return receivesTellStatic(player);
    }

    private boolean receivesTell(Player player) {
        return receivesTellStatic(player);
    }

    private static boolean receivesTellStatic(Player player) {
        Plugin utilidades = Bukkit.getPluginManager().getPlugin("UtilidadesPlus");
        if (utilidades == null || !utilidades.isEnabled()) return true;

        try {
            java.lang.reflect.Method method = utilidades.getClass().getMethod("receivesTell", Player.class);
            Object result = method.invoke(utilidades, player);
            return result instanceof Boolean value ? value : true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    private static String message(String path, String... replacements) {
        String value = plugin == null ? "" : plugin.getConfig().getString(path, "");
        if (value.isEmpty()) value = defaultMessage(path);

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }

        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private static String defaultMessage(String path) {
        return switch (path) {
            case "messages.tell.unknown-command" -> "&e&lᴄʜᴀᴛ &8• &rComando não encontrado.";
            case "messages.tell.usage" -> "&e&lᴄʜᴀᴛ &8• &rUse /tell <jogador> <mensagem>.";
            case "messages.tell.player-not-found" -> "&e&lᴄʜᴀᴛ &8• &cJogador não encontrado ou offline.";
            case "messages.tell.cannot-self" -> "&e&lᴄʜᴀᴛ &8• &cVocê não pode enviar uma mensagem para si mesmo.";
            case "messages.tell.target-disabled" -> "&e&lᴄʜᴀᴛ &8• &cO jogador desativou as mensagens privadas.";
            case "messages.tell.no-recent" -> "&e&lᴄʜᴀᴛ &8• &cVocê não possui nenhuma conversa privada recente.";
            case "messages.tell.player-only" -> "Comando disponível apenas para jogadores.";
            case "messages.tell.format-sender" -> "&e&lᴄʜᴀᴛ &8• &7Você → &f{player}&8: &r{message}";
            case "messages.tell.format-target" -> "&e&lᴄʜᴀᴛ &8• &7{player} → &fVocê&8: &r{message}";
            default -> "";
        };
    }
}
