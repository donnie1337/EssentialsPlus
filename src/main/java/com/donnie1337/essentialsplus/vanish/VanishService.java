package com.donnie1337.essentialsplus.vanish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishService {
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";
    private static final String VANISH_TAG = "<bold><gradient:#FFFFFF:#D9D9D9>[ɪɴᴠɪsɪᴠᴇʟ]</gradient></bold>";
    private static final double TAG_HEIGHT = 2.45D;

    private final Plugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Map<UUID, TextDisplay> vanishTags = new ConcurrentHashMap<>();
    private final BukkitTask tagTask;

    public VanishService(Plugin plugin) {
        this.plugin = plugin;
        this.tagTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateTags, 1L, 2L);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isVanished(Player player) {
        return player != null && vanished.contains(player.getUniqueId());
    }

    public boolean toggle(Player player) {
        if (player == null) return false;
        return setVanished(player, !isVanished(player));
    }

    public boolean setVanished(Player player, boolean value) {
        if (player == null) return false;
        if (value) vanished.add(player.getUniqueId());
        else vanished.remove(player.getUniqueId());

        if (value) {
            createTag(player);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) continue;
                if (canSeeVanished(viewer)) viewer.showPlayer(plugin, player);
                else viewer.hidePlayer(plugin, player);
            }
            setListed(player, false);
            updateTagVisibility(player);
        } else {
            removeTag(player);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) viewer.showPlayer(plugin, player);
            }
            setListed(player, true);
        }
        return true;
    }

    public void applyTo(Player player) {
        if (player == null) return;

        if (isVanished(player)) {
            createTag(player);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) continue;
                if (canSeeVanished(viewer)) viewer.showPlayer(plugin, player);
                else viewer.hidePlayer(plugin, player);
            }
            setListed(player, false);
        }

        for (Player vanishedPlayer : Bukkit.getOnlinePlayers()) {
            if (vanishedPlayer.equals(player) || !isVanished(vanishedPlayer)) continue;
            if (canSeeVanished(player)) player.showPlayer(plugin, vanishedPlayer);
            else player.hidePlayer(plugin, vanishedPlayer);
            TextDisplay tag = vanishTags.get(vanishedPlayer.getUniqueId());
            if (tag != null) {
                if (canSeeVanished(player)) player.showEntity(plugin, tag);
                else player.hideEntity(plugin, tag);
            }
        }
    }

    public void remove(Player player) {
        if (player == null) return;
        vanished.remove(player.getUniqueId());
        removeTag(player);
        setListed(player, true);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) viewer.showPlayer(plugin, player);
        }
    }

    public void clear() {
        for (Player player : Bukkit.getOnlinePlayers()) remove(player);
        for (TextDisplay tag : vanishTags.values()) {
            if (tag != null && !tag.isDead()) tag.remove();
        }
        vanishTags.clear();
        vanished.clear();
        if (tagTask != null) tagTask.cancel();
    }

    private void createTag(Player player) {
        if (player == null || !player.isOnline()) return;
        TextDisplay existing = vanishTags.get(player.getUniqueId());
        if (existing != null && !existing.isDead()) return;

        Location location = player.getLocation().clone().add(0.0D, TAG_HEIGHT, 0.0D);
        TextDisplay tag = player.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(createDisplayText(player));
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setTextOpacity((byte) -1);
            display.setLineWidth(300);
            display.setViewRange(32.0F);
            display.setVisibleByDefault(false);
        });
        vanishTags.put(player.getUniqueId(), tag);
        updateTagVisibility(player);
    }

    private Component createDisplayText(Player player) {
        String cargoPrefix = resolveCargoPrefix(player.getUniqueId());
        String nicknameColor = resolveCargoNicknameColor(player.getUniqueId());
        String legacy = cargoPrefix + nicknameColor + player.getName() + " ";
        Component name = LegacyComponentSerializer.legacySection().deserialize(legacy);
        Component invisible = MiniMessage.miniMessage().deserialize(VANISH_TAG);
        return name.append(invisible);
    }

    private String resolveCargoPrefix(UUID uuid) {
        Object api = resolveCargoApi();
        if (api == null) return "";
        try {
            Method method = api.getClass().getMethod("getPrefix", UUID.class);
            Object result = method.invoke(api, uuid);
            return result instanceof String ? (String) result : "";
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return "";
        }
    }

    private String resolveCargoNicknameColor(UUID uuid) {
        Object api = resolveCargoApi();
        if (api == null) return "§f";
        try {
            Method method = api.getClass().getMethod("getNicknameColor", UUID.class);
            Object result = method.invoke(api, uuid);
            return result instanceof String && !((String) result).isEmpty() ? (String) result : "§f";
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return "§f";
        }
    }

    private Object resolveCargoApi() {
        Plugin cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (cargo == null || !cargo.isEnabled()) return null;
        try {
            Method apiMethod = cargo.getClass().getMethod("api");
            Object api = apiMethod.invoke(cargo);
            if (api != null) return api;
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }

        try {
            Class<?> apiClass = Class.forName("com.cargoplus.api.CargoPlusAPI", false, cargo.getClass().getClassLoader());
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            return registration == null ? null : registration.getProvider();
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    private void removeTag(Player player) {
        if (player == null) return;
        TextDisplay tag = vanishTags.remove(player.getUniqueId());
        if (tag != null && !tag.isDead()) tag.remove();
    }

    private void updateTags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isVanished(player)) continue;
            TextDisplay tag = vanishTags.get(player.getUniqueId());
            if (tag == null || tag.isDead()) {
                createTag(player);
                tag = vanishTags.get(player.getUniqueId());
            }
            if (tag == null || tag.isDead()) continue;

            tag.teleport(player.getLocation().clone().add(0.0D, TAG_HEIGHT, 0.0D));
            tag.text(createDisplayText(player));
            updateTagVisibility(player);
        }
    }

    private void updateTagVisibility(Player vanishedPlayer) {
        TextDisplay tag = vanishTags.get(vanishedPlayer.getUniqueId());
        if (tag == null || tag.isDead()) return;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(vanishedPlayer)) continue;
            if (canSeeVanished(viewer)) viewer.showEntity(plugin, tag);
            else viewer.hideEntity(plugin, tag);
        }
    }

    private boolean canSeeVanished(Player viewer) {
        return viewer != null && viewer.hasPermission(VANISH_PERMISSION);
    }

    private void setListed(Player player, boolean listed) {
        try {
            Method method = player.getClass().getMethod("setListed", boolean.class);
            method.invoke(player, listed);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Older Bukkit/Paper APIs may not expose setListed; hidePlayer still provides vanish behavior.
        }
    }
}
