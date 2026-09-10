package com.donnie1337.essentialsplus.vanish;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
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
            display.text(MiniMessage.miniMessage().deserialize(VANISH_TAG));
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setTextOpacity((byte) -1);
            display.setLineWidth(200);
            display.setViewRange(32.0F);
            display.setVisibleByDefault(false);
        });
        vanishTags.put(player.getUniqueId(), tag);
        updateTagVisibility(player);
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
