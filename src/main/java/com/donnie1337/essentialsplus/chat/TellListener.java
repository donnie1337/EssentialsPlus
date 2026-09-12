package com.donnie1337.essentialsplus.chat;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TellListener implements Listener {
    public static final String CANCEL_BUTTON_ID = "essentialsplus:tell_cancel";
    private static final String PERMISSION = "essentialsplus.tell";
    private static final long PENDING_TIMEOUT_TICKS = 20L * 30L;
    private static final ConcurrentHashMap<UUID, UUID> LAST_TARGETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID> PENDING_TARGETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, TellState> TELL_STATES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, BukkitTask> PENDING_TIMEOUTS = new ConcurrentHashMap<>();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static JavaPlugin plugin;
    private static BukkitAudiences adventure;

    private enum TellState { PENDING, SENT, CANCELLED }

    public TellListener(JavaPlugin plugin, BukkitAudiences adventure) {
        TellListener.plugin = plugin;
        TellListener.adventure = adventure;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (event.getPlayer().hasPermission(PERMISSION)) {
            event.getCommands().add("tell");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTellCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2 || raw.charAt(0) != '/') return;

        String commandLine = raw.substring(1).trim();
        if (commandLine.isEmpty()) return;

        int separator = commandLine.indexOf(' ');
        String commandName = (separator < 0 ? commandLine : commandLine.substring(0, separator)).toLowerCase(Locale.ROOT);
        if (!commandName.equals("tell")) return;

        String argumentLine = separator < 0 ? "" : commandLine.substring(separator + 1).trim();
        String[] args = argumentLine.isEmpty() ? new String[0] : argumentLine.split("\\s+");

        event.setCancelled(true);
        handleTellCommand(event.getPlayer(), args);
    }

    public static boolean handleTellCommand(Player sender, String[] args) {
        if (sender == null) return true;
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(message("messages.tell.unknown-command"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(message("messages.tell.usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(message("messages.tell.player-not-found"));
            return true;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(message("messages.tell.cannot-self"));
            return true;
        }
        if (!receivesTellStatic(target)) {
            sender.sendMessage(message("messages.tell.target-disabled"));
            return true;
        }

        clearPending(sender.getUniqueId());
        if (args.length == 1) {
            PENDING_TARGETS.put(sender.getUniqueId(), target.getUniqueId());
            TELL_STATES.put(sender.getUniqueId(), TellState.PENDING);
            schedulePendingTimeout(sender.getUniqueId());
            sendPendingMessage(sender, target);
            return true;
        }

        sendPrivateMessage(sender, target, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID senderId = sender.getUniqueId();
        UUID targetId = PENDING_TARGETS.remove(senderId);
        if (targetId == null) return;

        cancelPendingTimeout(senderId);
        event.setCancelled(true);

        String privateMessage = event.getMessage();
        if (privateMessage == null || privateMessage.isBlank()) {
            TELL_STATES.put(senderId, TellState.PENDING);
            schedulePendingTimeout(senderId);
            sender.sendMessage(message("messages.tell.empty-message"));
            return;
        }

        TELL_STATES.put(senderId, TellState.SENT);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(message("messages.tell.player-not-found"));
                TELL_STATES.remove(senderId, TellState.SENT);
                return;
            }
            if (!receivesTellStatic(target)) {
                sender.sendMessage(message("messages.tell.target-disabled"));
                TELL_STATES.remove(senderId, TellState.SENT);
                return;
            }
            if (target.getUniqueId().equals(sender.getUniqueId())) {
                sender.sendMessage(message("messages.tell.cannot-self"));
                TELL_STATES.remove(senderId, TellState.SENT);
                return;
            }
            sendPrivateMessage(sender, target, privateMessage);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        clearPending(playerId);
        LAST_TARGETS.remove(playerId);
        TELL_STATES.remove(playerId);
    }

    public static void shutdown() {
        PENDING_TIMEOUTS.values().forEach(BukkitTask::cancel);
        PENDING_TIMEOUTS.clear();
        PENDING_TARGETS.clear();
        LAST_TARGETS.clear();
        TELL_STATES.clear();
        plugin = null;
        adventure = null;
    }

    public static Player getLastTarget(Player player) {
        UUID targetId = player == null ? null : LAST_TARGETS.get(player.getUniqueId());
        return targetId == null ? null : Bukkit.getPlayer(targetId);
    }

    public static void sendPrivateMessage(Player sender, Player target, String privateMessage) {
        if (sender == null || target == null || privateMessage == null || privateMessage.isBlank()) return;
        clearPending(sender.getUniqueId());
        TELL_STATES.put(sender.getUniqueId(), TellState.SENT);
        LAST_TARGETS.put(sender.getUniqueId(), target.getUniqueId());
        LAST_TARGETS.put(target.getUniqueId(), sender.getUniqueId());

        String targetName = coloredCargoName(target);
        String senderName = coloredCargoName(sender);
        sender.sendMessage(message("messages.tell.format-sender", "player", targetName, "message", privateMessage));
        target.sendMessage(message("messages.tell.format-target", "player", senderName, "message", privateMessage));
    }

    public enum CancelResult { CANCELLED, ALREADY_CANCELLED, ALREADY_SENT }

    public static CancelResult cancelPendingTell(Player player) {
        if (player == null) return CancelResult.ALREADY_CANCELLED;
        UUID playerId = player.getUniqueId();
        UUID pending = PENDING_TARGETS.remove(playerId);
        if (pending != null) {
            cancelPendingTimeout(playerId);
            TELL_STATES.put(playerId, TellState.CANCELLED);
            return CancelResult.CANCELLED;
        }
        if (TELL_STATES.get(playerId) == TellState.SENT) return CancelResult.ALREADY_SENT;
        return CancelResult.ALREADY_CANCELLED;
    }

    public static boolean canReceiveTell(Player player) { return receivesTellStatic(player); }

    private static void schedulePendingTimeout(UUID playerId) {
        cancelPendingTimeout(playerId);
        if (plugin == null) return;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (PENDING_TARGETS.remove(playerId) != null) {
                TELL_STATES.put(playerId, TellState.CANCELLED);
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) player.sendMessage(message("messages.tell.expired"));
            }
            PENDING_TIMEOUTS.remove(playerId);
        }, PENDING_TIMEOUT_TICKS);
        PENDING_TIMEOUTS.put(playerId, task);
    }

    private static void cancelPendingTimeout(UUID playerId) {
        BukkitTask task = PENDING_TIMEOUTS.remove(playerId);
        if (task != null) task.cancel();
    }

    private static void clearPending(UUID playerId) {
        PENDING_TARGETS.remove(playerId);
        cancelPendingTimeout(playerId);
    }

    private static String coloredCargoName(Player player) {
        if (player == null) return "";
        return cargoColor(player) + player.getName() + ChatColor.RESET;
    }

    private static String cargoColor(Player player) {
        Plugin cargoPlus = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (cargoPlus == null || !cargoPlus.isEnabled()) return ChatColor.WHITE.toString();
        try {
            Class<?> apiClass = Class.forName("com.cargoplus.api.CargoPlusAPI");
            Object registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration != null) {
                Method getProvider = registration.getClass().getMethod("getProvider");
                Object api = getProvider.invoke(registration);
                if (api != null) {
                    Method getNicknameColor = apiClass.getMethod("getNicknameColor", UUID.class);
                    Object color = getNicknameColor.invoke(api, player.getUniqueId());
                    if (color instanceof String colorValue && !colorValue.isBlank()) return colorValue;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        try {
            Method permissionsMethod = cargoPlus.getClass().getMethod("permissions");
            Object permissions = permissionsMethod.invoke(cargoPlus);
            if (permissions == null) return ChatColor.WHITE.toString();
            Method getGroupMethod = permissions.getClass().getMethod("getGroup", UUID.class);
            Object group = getGroupMethod.invoke(permissions, player.getUniqueId());
            if (!(group instanceof String groupName) || groupName.isBlank()) return ChatColor.WHITE.toString();
            Method colorMethod = cargoPlus.getClass().getMethod("getCargoColor", String.class);
            Object color = colorMethod.invoke(cargoPlus, groupName);
            if (color instanceof String colorValue && !colorValue.isBlank()) return colorValue;
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        return ChatColor.WHITE.toString();
    }

    private static void sendPendingMessage(Player sender, Player target) {
        String raw = plugin.getConfig().getString("messages.tell.awaiting-message", "&d&lᴛᴇʟʟ &8• &rJogador {player} encontrado, digite alguma mensagem no chat para enviar, ou clique {cancel} para cancelar o envio.");
        raw = raw.replace("{player}", coloredCargoName(target));
        String cancelText = plugin.getConfig().getString("messages.tell.cancel-text", "&c&lAQUI");
        String cancelHover = plugin.getConfig().getString("messages.tell.cancel-hover", "&7Clique para cancelar o envio.");
        String[] parts = raw.split("\\{cancel}", -1);
        if (parts.length != 2) {
            adventure.player(sender).sendMessage(LEGACY.deserialize(raw));
            return;
        }
        Component message = LEGACY.deserialize(parts[0])
                .append(LEGACY.deserialize(cancelText)
                        .clickEvent(ClickEvent.custom(Key.key(CANCEL_BUTTON_ID)))
                        .hoverEvent(LEGACY.deserialize(cancelHover)))
                .append(LEGACY.deserialize(parts[1]));
        adventure.player(sender).sendMessage(message);
    }

    public static String message(String path, String... replacements) {
        String value = plugin == null ? "" : plugin.getConfig().getString(path, "");
        if (value.isEmpty()) value = defaultMessage(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private static boolean receivesTellStatic(Player player) {
        Plugin utilidades = Bukkit.getPluginManager().getPlugin("UtilidadesPlus");
        if (utilidades == null || !utilidades.isEnabled()) return true;
        try {
            Method method = utilidades.getClass().getMethod("receivesTell", Player.class);
            Object result = method.invoke(utilidades, player);
            return result instanceof Boolean value ? value : true;
        } catch (ReflectiveOperationException | LinkageError ignored) { return true; }
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
            case "messages.tell.awaiting-message" -> "&d&lᴛᴇʟʟ &8• &rJogador {player} encontrado, digite alguma mensagem no chat para enviar, ou clique {cancel} para cancelar o envio.";
            case "messages.tell.cancel-text" -> "&c&lAQUI";
            case "messages.tell.cancel-hover" -> "&7Clique para cancelar o envio.";
            case "messages.tell.cancelled" -> "&d&lᴛᴇʟʟ &8• &rEnvio cancelado.";
            case "messages.tell.already-cancelled" -> "&d&lᴛᴇʟʟ &8• &rNão foi possível cancelar, pois você já cancelou o envio.";
            case "messages.tell.already-sent" -> "&d&lᴛᴇʟʟ &8• &rVocê enviou uma mensagem, não foi possível cancelar.";
            case "messages.tell.empty-message" -> "&d&lᴛᴇʟʟ &8• &rA mensagem não pode estar vazia. Digite novamente para enviar ou aguarde o cancelamento automático.";
            case "messages.tell.expired" -> "&d&lᴛᴇʟʟ &8• &rO envio expirou por falta de resposta.";
            default -> "";
        };
    }
}
