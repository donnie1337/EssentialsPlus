package com.donnie1337.essentialsplus.vanish;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

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

    /**
     * Sends the vanish event through ChatPlus so /v uses exactly the same
     * staff-chat format as /s, including the CargoPlus prefix, nickname color
     * and the existing hover on the cargo. EssentialsPlus does not construct
     * another [S] message itself, avoiding duplicated or broken CargoPlus
     * gradients.
     */
    private void broadcastStaff(Player player, String message) {
        Plugin chatPlus = player.getServer().getPluginManager().getPlugin("ChatPlus");
        if (chatPlus == null || !chatPlus.isEnabled()) return;

        try {
            Method method = chatPlus.getClass().getMethod("sendStaffSystemMessage", Player.class, String.class);
            method.invoke(chatPlus, player, message);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // ChatPlus is optional; if its integration API is unavailable,
            // do not fall back to a second, differently formatted staff chat.
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
