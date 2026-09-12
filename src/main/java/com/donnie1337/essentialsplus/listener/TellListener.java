package com.donnie1337.essentialsplus.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TellListener implements Listener {
    public static final String CANCEL_BUTTON_ID = "essentialsplus:tell_cancel";
    private static final String PERMISSION = "essentialsplus.tell";
    private static final ConcurrentHashMap<UUID, UUID> LAST_TARGETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID> PENDING_TARGETS = new ConcurrentHashMap<>();
    private static JavaPlugin plugin;

    public TellListener(JavaPlugin plugin) { TellListener.plugin = plugin; }

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
        if (args.length < 2) {
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

        if (args.length < 3) {
            PENDING_TARGETS.put(sender.getUniqueId(), target.getUniqueId());
            sendPendingMessage(sender, target);
            return;
        }
        sendPrivateMessage(sender, target, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID targetId = PENDING_TARGETS.remove(event.getPlayer().getUniqueId());
        if (targetId == null) return;
        Player sender = event.getPlayer();
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            sender.sendMessage(message("messages.tell.player-not-found"));
            return;
        }
        event.setCancelled(true);
        sendPrivateMessage(sender, target, event.getMessage());
    }

    private void sendPendingMessage(Player sender, Player target) {
        String targetName = coloredCargoName(target);
        Component cancel = Component.text(message("messages.tell.cancel-text"))
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.customClickEvent(CANCEL_BUTTON_ID))
                .hoverEvent(HoverEvent.showText(Component.text(message("messages.tell.cancel-hover"))));
        sender.sendMessage(Component.text(message("messages.tell.awaiting-message", "player", targetName, "cancel", ""))
                .append(cancel));
    }

    public static void cancelPending(Player player) {
        UUID pending = PENDING_TARGETS.remove(player.getUniqueId());
        if (pending == null) {
            player.sendMessage(message("messages.tell.already-cancelled"));
            return;
        }
        player.sendMessage(message("messages.tell.cancelled"));
    }

    public static void sendPrivateMessage(Player sender, Player target, String privateMessage) {
        PENDING_TARGETS.remove(sender.getUniqueId());
        LAST_TARGETS.put(sender.getUniqueId(), target.getUniqueId());
        LAST_TARGETS.put(target.getUniqueId(), sender.getUniqueId());

        String targetName = coloredCargoName(target);
        String senderName = coloredCargoName(sender);
        sender.sendMessage(message("messages.tell.format-sender", "player", targetName, "message", privateMessage));
        target.sendMessage(message("messages.tell.format-target", "player", senderName, "message", privateMessage));
    }

    public static Player getLastTarget(Player player) {
        UUID target = LAST_TARGETS.get(player.getUniqueId());
        return target == null ? null : Bukkit.getPlayer(target);
    }

    private boolean receivesTell(Player player) { return true; }

    private static String coloredCargoName(Player player) {
        try {
            Class<?> api = Class.forName("com.cargoplus.api.CargoPlusAPI");
            Object result = api.getMethod("getColoredName", Player.class).invoke(null, player);
            if (result instanceof String value && !value.isBlank()) return value;
        } catch (Throwable ignored) { }
        return player.getName();
    }

    public static String message(String path, String... replacements) {
        String value = plugin == null ? "" : plugin.getConfig().getString(path, "");
        if (value.isEmpty()) value = defaultMessage(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private static String defaultMessage(String path) {
        return switch (path) {
            case "messages.tell.unknown-command" -> "&d&lᴛᴇʟʟ &8• &rComando não encontrado.";
            case "messages.tell.usage" -> "&d&lᴛᴇʟʟ &8• &rUse /tell <jogador> [mensagem].";
            case "messages.tell.reply-usage" -> "&d&lᴛᴇʟʟ &8• &rUse /r <mensagem>.";
            case "messages.tell.player-not-found" -> "&d&lᴛᴇʟʟ &8• &cJogador não encontrado.";
            case "messages.tell.cannot-self" -> "&d&lᴛᴇʟʟ &8• &cVocê não pode enviar uma mensagem para si mesmo.";
            case "messages.tell.target-disabled" -> "&d&lᴛᴇʟʟ &8• &cO jogador desativou as mensagens privadas.";
            case "messages.tell.no-recent" -> "&d&lᴛᴇʟʟ &8• &cVocê não possui nenhuma conversa privada recente.";
            case "messages.tell.player-only" -> "Comando disponível apenas para jogadores.";
            case "messages.tell.format-sender" -> "&d&lᴛᴇʟʟ &8• &7Você → &f{player}&8: &r{message}";
            case "messages.tell.format-target" -> "&d&lᴛᴇʟʟ &8• &7{player} → &fVocê&8: &r{message}";
            case "messages.tell.awaiting-message" -> "&d&lᴛᴇʟʟ &8• &rJogador {player} encontrado, digite alguma mensagem no chat para enviar, ou clique ";
            case "messages.tell.cancel-text" -> "AQUI";
            case "messages.tell.cancel-hover" -> "&7Clique para cancelar o envio.";
            case "messages.tell.cancelled" -> "&d&lᴛᴇʟʟ &8• &rEnvio cancelado.";
            case "messages.tell.already-cancelled" -> "&d&lᴛᴇʟʟ &8• &rVocê já cancelou o envio.";
            default -> "";
        };
    }
}
