package com.donnie1337.essentialsplus.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Optional integration with ChatPlus. EssentialsPlus never links directly
 * against ChatPlus classes, so the TPA system remains functional when
 * ChatPlus is not installed or is updated independently.
 */
public final class ChatPlusBridge {
    private static final String PLUGIN_NAME = "ChatPlus";

    private volatile Plugin chatPlus;
    private volatile Method sendSystemMessage;
    private volatile boolean lookupComplete;

    public boolean sendSystemMessage(Player player, Component message) {
        if (player == null || !player.isOnline() || message == null) return false;

        final Plugin plugin = resolvePlugin();
        final Method method = sendSystemMessage;
        if (plugin == null || method == null) return false;

        try {
            method.invoke(plugin, player, message);
            return true;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return false;
        }
    }

    private Plugin resolvePlugin() {
        final Plugin current = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (current == null || !current.isEnabled()) {
            chatPlus = null;
            sendSystemMessage = null;
            lookupComplete = true;
            return null;
        }

        if (!lookupComplete || chatPlus != current) {
            synchronized (this) {
                if (!lookupComplete || chatPlus != current) {
                    chatPlus = current;
                    try {
                        sendSystemMessage = current.getClass().getMethod(
                                "sendSystemMessage", Player.class, Component.class);
                    } catch (NoSuchMethodException ignored) {
                        sendSystemMessage = null;
                    }
                    lookupComplete = true;
                }
            }
        }

        return chatPlus;
    }
}
