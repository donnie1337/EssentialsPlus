package com.donnie1337.essentialsplus.inspect;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class VerCommand implements CommandExecutor {
    private static final String PERMISSION = "essentialsplus.inspect";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rUse /ver <jogador>.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rJogador não encontrado ou offline.");
            return true;
        }

        InspectHolder holder = new InspectHolder(target.getUniqueId(), InspectHolder.Type.PLAYER);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§8Inventário de " + target.getName());
        holder.setInventory(inventory);

        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = target.getInventory().getItem(slot);
            if (item != null) inventory.setItem(slot, item.clone());
        }
        inventory.setItem(45, target.getInventory().getHelmet() == null ? null : target.getInventory().getHelmet().clone());
        inventory.setItem(46, target.getInventory().getChestplate() == null ? null : target.getInventory().getChestplate().clone());
        inventory.setItem(47, target.getInventory().getLeggings() == null ? null : target.getInventory().getLeggings().clone());
        inventory.setItem(48, target.getInventory().getBoots() == null ? null : target.getInventory().getBoots().clone());
        inventory.setItem(49, target.getInventory().getItemInOffHand().clone());

        player.openInventory(inventory);
        return true;
    }
}
