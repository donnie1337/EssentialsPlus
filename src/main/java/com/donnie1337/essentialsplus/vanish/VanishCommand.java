package com.donnie1337.essentialsplus.vanish;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class VanishCommand implements CommandExecutor, TabCompleter {
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";

    private final VanishService service;

    public VanishCommand(VanishService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission(VANISH_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para executar este comando.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(ChatColor.RED + "Uso: /v");
            return true;
        }

        service.toggle(player);
        boolean enabled = service.isVanished(player);

        if (enabled) {
            player.sendMessage(ChatColor.RED + "§lATENÇÃO " + ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Você ficou invisível para outros jogadores.");
            broadcastStaff(player, "FICOU INVISÍVEL.");
        } else {
            player.sendMessage(ChatColor.GREEN + "§lATENÇÃO " + ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Você voltou a ficar visível para outros jogadores.");
            broadcastStaff(player, "NÃO ESTÁ MAIS INVISÍVEL.");
        }
        return true;
    }

    private void broadcastStaff(Player player, String message) {
        String staffMessage = ChatColor.DARK_GRAY + "[S] "
                + resolveCargoPrefix(player.getUniqueId())
                + resolveCargoNicknameColor(player.getUniqueId())
                + player.getName().toUpperCase(Locale.ROOT)
                + ChatColor.DARK_GRAY + ": "
                + ChatColor.WHITE + message;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(VANISH_PERMISSION)) {
                viewer.sendMessage(staffMessage);
            }
        }
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
        if (api == null) return ChatColor.WHITE.toString();
        try {
            Method method = api.getClass().getMethod("getNicknameColor", UUID.class);
            Object result = method.invoke(api, uuid);
            return result instanceof String && !((String) result).isEmpty() ? (String) result : ChatColor.WHITE.toString();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return ChatColor.WHITE.toString();
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
            org.bukkit.plugin.RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            return registration == null ? null : registration.getProvider();
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
