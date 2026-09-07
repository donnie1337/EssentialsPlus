package com.donnie1337.essentialsplus.auth;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Integração opcional com o AuthSystem sem dependência de compilação.
 * Quando o AuthSystem está presente, ações do EssentialsPlus exigem autenticação.
 */
public final class AuthSystemBridge {
    private static final String PLUGIN_NAME = "AuthSystem";

    private volatile Plugin authSystem;
    private volatile Method isAuthenticated;
    private volatile boolean lookupComplete;

    public boolean isAuthenticated(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        final Plugin plugin = resolvePlugin();
        final Method method = isAuthenticated;
        if (plugin == null) {
            // O EssentialsPlus continua independente quando o AuthSystem não está instalado.
            return true;
        }
        if (method == null) {
            // Se o AuthSystem está instalado, mas sua API de autenticação não está disponível,
            // nunca permita que a integração falhe-aberta.
            return false;
        }

        try {
            final Object result = method.invoke(plugin, player);
            return result instanceof Boolean authenticated && authenticated;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            // Falha na integração deve ser fail-closed quando o AuthSystem está presente.
            return false;
        }
    }

    private Plugin resolvePlugin() {
        final Plugin current = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (current == null || !current.isEnabled()) {
            authSystem = null;
            isAuthenticated = null;
            lookupComplete = true;
            return null;
        }

        if (!lookupComplete || authSystem != current) {
            synchronized (this) {
                if (!lookupComplete || authSystem != current) {
                    authSystem = current;
                    try {
                        isAuthenticated = current.getClass().getMethod("isAuthenticated", Player.class);
                    } catch (NoSuchMethodException ignored) {
                        isAuthenticated = null;
                    }
                    lookupComplete = true;
                }
            }
        }

        return authSystem;
    }
}
