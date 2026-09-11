package com.donnie1337.essentialsplus.command;

import com.donnie1337.essentialsplus.inspect.InspectHolder;
import com.donnie1337.essentialsplus.inspect.LiveInspectionService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class EcCommand implements CommandExecutor {
    private static final String PERMISSION = "essentialsplus.ec";
    private static final String INSPECT_PERMISSION = "essentialsplus.inspect";

    private final LiveInspectionService liveInspectionService;

    public EcCommand(LiveInspectionService liveInspectionService) {
        this.liveInspectionService = liveInspectionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (args.length == 0) {
            if (!player.hasPermission(PERMISSION)) {
                player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
                return true;
            }
            player.openInventory(player.getEnderChest());
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rJogador não encontrado ou offline.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            if (!player.hasPermission(PERMISSION)) {
                player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
                return true;
            }
            player.openInventory(player.getEnderChest());
            return true;
        }

        if (!player.hasPermission(INSPECT_PERMISSION)) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        InspectHolder holder = new InspectHolder(target.getUniqueId(), InspectHolder.Type.ENDER_CHEST);
        Inventory inventory = Bukkit.createInventory(holder, 27, "§8Ender Chest de " + target.getName());
        holder.setInventory(inventory);

        for (int slot = 0; slot < 27; slot++) {
            ItemStack item = target.getEnderChest().getItem(slot);
            if (item != null) inventory.setItem(slot, item.clone());
        }

        player.openInventory(inventory);
        liveInspectionService.register(player, inventory);
        return true;
    }
}
