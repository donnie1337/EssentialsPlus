package com.donnie1337.essentialsplus.chat;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TellListener implements Listener {
    public static final String CANCEL_BUTTON_ID = "essentialsplus:tell_cancel";
    private static final String PERMISSION = "essentialsplus.tell";
    private static final ConcurrentHashMap<UUID, UUID> LAST_TARGETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID> PENDING_TARGETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, TellState> TELL_STATES = new ConcurrentHashMap<>();
    private static JavaPlugin plugin;

    private enum TellState {
        PENDING,
        SENT,
        CANCELLED
    }

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
            TELL_STATES.put(sender.getUniqueId(), TellState.PENDING);
            sendPendingMessage(sender, target);
            return;
        }
        sendPrivateMessage(sender, target, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID targetId = PENDING_TARGETS.get(sender.getUniqueId());
        if (targetId == null) return;

        event.setCancelled(true);
        PENDING_TARGETS.remove(sender.getUniqueId(), targetId);

        String privateMessage = event.getMessage();
        if (privateMessage == null || privateMessage.isBlank()) return;

        // A mensagem já foi digitada/enviada; qualquer clique posterior no AQUI
        // deve informar que o cancelamento não é mais possível.
        TELL_STATES.put(sender.getUniqueId(), TellState.SENT);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(message("messages.tell.player-not-found"));
                return;
            }
            if (!receivesTellStatic(target)) {
                sender.sendMessage(message("messages.tell.target-disabled"));
                return;
            }
            if (target.getUniqueId().equals(sender.getUniqueId())) {
                sender.sendMessage(message("messages.tell.cannot-self"));
                return;
            }
            sendPrivateMessage(sender, target, privateMessage);
        });
    }

    public static Player getLastTarget(Player player) {
        UUID targetId = player == null ? null : LAST_TARGETS.get(player.getUniqueId());
        return targetId == null ? null : Bukkit.getPlayer(targetId);
    }

    public static void sendPrivateMessage(Player sender, Player target, String privateMessage) {
        PENDING_TARGETS.remove(sender.getUniqueId());
        TELL_STATES.put(sender.getUniqueId(), TellState.SENT);
        LAST_TARGETS.put(sender.getUniqueId(), target.getUniqueId());
        LAST_TARGETS.put(target.getUniqueId(), sender.getUniqueId());

        String targetName = coloredCargoName(target);
        String senderName = coloredCargoName(sender);
        sender.sendMessage(message("messages.tell.format-sender", "player", targetName, "message", privateMessage));
        target.sendMessage(message("messages.tell.format-target", "player", senderName, "message", privateMessage));
    }

    public enum CancelResult {
        CANCELLED,
        ALREADY_CANCELLED,
        ALREADY_SENT
    }

    public static CancelResult cancelPendingTell(Player player) {
        if (player == null) return CancelResult.ALREADY_CANCELLED;

        UUID playerId = player.getUniqueId();
        UUID pending = PENDING_TARGETS.remove(playerId);
        if (pending != null) {
            TELL_STATES.put(playerId, TellState.CANCELLED);
            return CancelResult.CANCELLED;
        }

        TellState state = TELL_STATES.get(playerId);
        if (state == TellState.SENT) return CancelResult.ALREADY_SENT;
        return CancelResult.ALREADY_CANCELLED;
    }

    public static boolean canReceiveTell(Player player) {
        return receivesTellStatic(player);
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
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
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
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return ChatColor.WHITE.toString();
    }

    private static void sendPendingMessage(Player sender, Player target) {
        String raw = plugin.getConfig().getString("messages.tell.awaiting-message", "&d&lᴛᴇʟʟ &8• &rJogador {player} encontrado, digite alguma mensagem no chat para enviar, ou clique {cancel} para cancelar o envio.");
        raw = raw.replace("{player}", coloredCargoName(target));
        String cancelText = plugin.getConfig().getString("messages.tell.cancel-text", "&c&lAQUI");
        String cancelHover = plugin.getConfig().getString("messages.tell.cancel-hover", "&7Clique para cancelar o envio.");
        String[] parts = raw.split("\\{cancel}", -1);
        if (parts.length != 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', raw));
            return;
        }

        java.util.List<BaseComponent> components = new java.util.ArrayList<>();
        components.addAll(Arrays.asList(legacyComponents(parts[0])));
        components.addAll(Arrays.asList(cancelButton(cancelText, cancelHover)));
        components.addAll(Arrays.asList(legacyComponents(parts[1])));
        sender.spigot().sendMessage(components.toArray(new BaseComponent[0]));
    }

    private static BaseComponent[] legacyComponents(String text) {
        String translated = ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
        return TextComponent.fromLegacyText(translated);
    }

    private static BaseComponent[] cancelButton(String text, String hover) {
        BaseComponent[] components = legacyComponents(text);
        ClickEvent click = createCustomClick();
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, legacyComponents(hover));
        for (BaseComponent component : components) {
            if (click != null) component.setClickEvent(click);
            component.setHoverEvent(hoverEvent);
        }
        return components;
    }

    private static ClickEvent createCustomClick() {
        try {
            Class<?> type = Class.forName("net.md_5.bungee.api.chat.ClickEventCustom");
            Constructor<?> constructor = type.getConstructor(String.class, String.class);
            return (ClickEvent) constructor.newInstance(CANCEL_BUTTON_ID, "cancel");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    public static String message(String path, String... replacements) {
        String value = plugin == null ? "" : plugin.getConfig().getString(path, "");
        if (value.isEmpty()) value = defaultMessage(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private boolean receivesTell(Player player) { return receivesTellStatic(player); }

    private static boolean receivesTellStatic(Player player) {
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
            default -> "";
        };
    }
}
