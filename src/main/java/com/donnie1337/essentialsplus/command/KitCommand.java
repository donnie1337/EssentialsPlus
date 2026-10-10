package com.donnie1337.essentialsplus.command;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class KitCommand implements CommandExecutor, Listener {

    private static final NamespacedKey TERRAIN_TOOL_KEY = new NamespacedKey("terrenosplus", "tool");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("terreno")) {
            player.sendMessage(color("&e[Kit] &fUse: &e/kit terreno"));
            return true;
        }

        giveTerrainKit(player);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTerrainKitCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage().trim();
        if (!raw.equalsIgnoreCase("/kit terreno")) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (!player.hasPermission("essentialsplus.kit")) {
            player.sendMessage(color("&c[Erro] &cComando não encontrado."));
            return;
        }

        giveTerrainKit(player);
    }

    private void giveTerrainKit(Player player) {
        give(player, protectionShovel());
        give(player, trackingStick());
        player.sendMessage(color("&a[Kit] &fVocê recebeu o kit &aTerreno&f."));
    }

    private ItemStack protectionShovel() {
        ItemStack item = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&ePá de Proteção"));
        meta.setLore(List.of(
                "",
                color("&fUse este item para proteger seus terrenos"),
                color("&fe construir suas casas com proteção"),
                "",
                color("&7Como usar:"),
                color("&fClique com botão direito em &e2 pontos"),
                color("&fdiagonais distintos de um terreno"),
                "",
                color("&aClique em um bloco no chão para usar")
        ));
        meta.getPersistentDataContainer().set(TERRAIN_TOOL_KEY, PersistentDataType.STRING, "claim");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack trackingStick() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&ePalito de Rastreamento"));
        meta.setLore(List.of(
                "",
                color("&fUse este item para verificar terrenos"),
                color("&fprotegidos e suas informações"),
                "",
                color("&7Funcionalidades:"),
                color("&e▪ &fDescubra quem é o dono de um terreno"),
                color("&e▪ &fVeja os limites do seu terreno"),
                "",
                color("&aClique em um bloco para verificar")
        ));
        meta.getPersistentDataContainer().set(TERRAIN_TOOL_KEY, PersistentDataType.STRING, "inspect");
        item.setItemMeta(meta);
        return item;
    }

    private void give(Player player, ItemStack item) {
        var leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
