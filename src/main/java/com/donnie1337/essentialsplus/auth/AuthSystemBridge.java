package com.donnie1337.essentialsplus.auth;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Integração opcional com o LoginPlus sem dependência de compilação.
 * Quando o LoginPlus está presente, ações do EssentialsPlus exigem autenticação.
 */
public final class AuthSystemBridge {
    private static final String PLUGIN_NAME = "LoginPlus";

    private volatile Plugin authSystem;
    private volatile Method isAuthenticated;
    private volatile boolean lookupComplete;

    public boolean isAuthenticated(Player player) {
        if (player == null || !player.isOnline()) return false;

        final Plugin current = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (current == null) {
            // O EssentialsPlus continua independente quando o LoginPlus não está instalado.
            clearCachedIntegration();
            return true;
        }
        if (!current.isEnabled()) {
            // LoginPlus instalado, mas desabilitado: nunca falhar-aberto.
            clearCachedIntegration();
            return false;
        }

        final Plugin plugin = resolvePlugin(current);
        final Method method = isAuthenticated;
        if (plugin == null || method == null) return false;

        try {
            final Object result = method.invoke(plugin, player);
            return result instanceof Boolean authenticated && authenticated;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            // Falha na integração deve ser fail-closed quando o LoginPlus está presente.
            return false;
        }
    }

    private Plugin resolvePlugin(Plugin current) {
        if (!lookupComplete || authSystem != current || isAuthenticated == null) {
            synchronized (this) {
                if (!lookupComplete || authSystem != current || isAuthenticated == null) {
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

    private synchronized void clearCachedIntegration() {
        authSystem = null;
        isAuthenticated = null;
        lookupComplete = false;
    }
}
