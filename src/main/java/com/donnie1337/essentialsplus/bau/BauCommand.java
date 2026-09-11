package com.donnie1337.essentialsplus.bau;

import com.donnie1337.essentialsplus.inspect.InspectHolder;
import com.donnie1337.essentialsplus.inspect.LiveInspectionService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class BauCommand implements CommandExecutor {
    private static final String PERMISSION = "essentialsplus.bau";
    private static final String INSPECT_PERMISSION = "essentialsplus.inspect";

    private final BauService bauService;
    private final LiveInspectionService liveInspectionService;

    public BauCommand(BauService bauService, LiveInspectionService liveInspectionService) {
        this.bauService = bauService;
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

            player.openInventory(bauService.createInventory(player));
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

            player.openInventory(bauService.createInventory(player));
            return true;
        }

        if (!player.hasPermission(INSPECT_PERMISSION)) {
            player.sendMessage("§e§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }

        InspectHolder holder = new InspectHolder(target.getUniqueId(), InspectHolder.Type.BAU);
        Inventory inventory = Bukkit.createInventory(holder, BauService.SIZE, "§8Baú de " + target.getName());
        holder.setInventory(inventory);
        bauService.loadSaved(target.getUniqueId(), inventory);

        player.openInventory(inventory);
        liveInspectionService.register(player, inventory);
        return true;
    }
}
