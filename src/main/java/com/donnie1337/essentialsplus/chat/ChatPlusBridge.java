package com.donnie1337.essentialsplus.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/** Integration with ChatPlus. */
public final class ChatPlusBridge {
    private Plugin plugin;
    private Method sendSystemMessage;

    public boolean connect() {
        Plugin found = Bukkit.getPluginManager().getPlugin("ChatPlus");
        if (found == null || !found.isEnabled()) return false;
        try {
            Method method = found.getClass().getMethod("sendSystemMessage", Player.class, String.class);
            this.plugin = found;
            this.sendSystemMessage = method;
            return true;
        } catch (ReflectiveOperationException ignored) {
            this.plugin = null;
            this.sendSystemMessage = null;
            return false;
        }
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled() && sendSystemMessage != null;
    }

    public boolean sendSystemMessage(Player player, String message) {
        if (player == null || message == null) return false;
        if (!isAvailable() && !connect()) return false;
        try {
            sendSystemMessage.invoke(plugin, player, message);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    /** Sends directly to the player, bypassing ChatPlus's global system prefix. */
    public boolean sendDirect(Player player, String message) {
        if (player == null || !player.isOnline() || message == null) return false;
        player.sendMessage(message);
        return true;
    }
}
