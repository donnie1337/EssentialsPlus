package com.donnie1337.essentialsplus.home;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Map;

public final class HomeGui implements Listener {
    private static final int SIZE = 27;
    private static final int INTRO_SLOT = 13;

    private final HomeService service;

    public HomeGui(HomeService service) {
        this.service = service;
    }

    public void openIntro(Player player) {
        Inventory inventory = Bukkit.createInventory(new HomesHolder(HomesHolder.Type.INTRO), SIZE, "Suas homes");

        ItemStack item = new ItemStack(Material.DIRT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSuas homes");
        meta.setLore(java.util.List.of(
                "§7Gerencie todos os pontos de",
                "§7teleporte personalizados.",
                "",
                "§aClique para gerenciar"
        ));
        item.setItemMeta(meta);
        inventory.setItem(INTRO_SLOT, item);

        player.openInventory(inventory);
    }

    public void openHomes(Player player) {
        Inventory inventory = Bukkit.createInventory(new HomesHolder(HomesHolder.Type.HOMES), SIZE, "Suas homes");
        Map<String, Home> homes = service.homes(player);

        int slot = 0;
        for (Home home : homes.values()) {
            if (slot >= SIZE) break;

            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + home.name());
            ArrayList<String> lore = new ArrayList<>();
            lore.add("§7Mundo: §f" + home.location().getWorld().getName());
            lore.add("§7X: §f" + format(home.location().getX()) + " §7Y: §f" + format(home.location().getY()) + " §7Z: §f" + format(home.location().getZ()));
            lore.add("");
            lore.add("§aClique para teleportar");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }

        if (homes.isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§cNenhuma home encontrada");
            meta.setLore(java.util.List.of("§7Use §f/sethome <nome> §7para criar uma."));
            item.setItemMeta(meta);
            inventory.setItem(13, item);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof HomesHolder holder)) return;

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        if (holder.type() == HomesHolder.Type.INTRO && event.getRawSlot() == INTRO_SLOT) {
            openHomes(player);
            return;
        }

        if (holder.type() != HomesHolder.Type.HOMES) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.GRASS_BLOCK || clicked.getItemMeta() == null) return;

        String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        try {
            Home home = service.getHome(player, name);
            if (home == null || home.location().getWorld() == null) {
                player.sendMessage("§cEsta home nao esta mais disponivel.");
                openHomes(player);
                return;
            }
            player.closeInventory();
            player.teleport(home.location());
            player.sendMessage("§aTeleportado para a home §f" + home.name() + "§a.");
        } catch (IllegalArgumentException exception) {
            player.sendMessage("§cNao foi possivel carregar esta home.");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof HomesHolder) event.setCancelled(true);
    }

    private String format(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private record HomesHolder(Type type) implements InventoryHolder {
        private enum Type { INTRO, HOMES }
        @Override
        public Inventory getInventory() { return null; }
    }
}
