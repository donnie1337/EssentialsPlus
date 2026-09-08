package com.donnie1337.essentialsplus.home;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HomeGui implements Listener {
    private static final int INTRO_SIZE = 27;
    private static final int HOMES_SIZE = 54;
    private static final int MANAGE_SIZE = 36;
    private static final int INTRO_SLOT = 13;
    private static final int FIRST_HOME_SLOT = 11;
    private static final int ALTER_NAME_SLOT = 13;
    private static final int BACK_SLOT = 49;
    private static final int MANAGE_BACK_SLOT = 31;

    private final HomeService service;
    private final Map<UUID, String> pendingRenames = new HashMap<>();

    public HomeGui(HomeService service) {
        this.service = service;
    }

    public void openIntro(Player player) {
        Inventory inventory = Bukkit.createInventory(new HomesHolder(HomesHolder.Type.INTRO, null), INTRO_SIZE, "Homes");

        ItemStack item = new ItemStack(Material.DIRT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSuas homes");
        meta.setLore(List.of(
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
        Inventory inventory = Bukkit.createInventory(new HomesHolder(HomesHolder.Type.HOMES, null), HOMES_SIZE, "Homes -> Suas Homes");
        Map<String, Home> homes = service.homes(player);

        int slot = FIRST_HOME_SLOT;
        for (Home home : homes.values()) {
            if (slot >= BACK_SLOT) break;

            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + home.name());
            meta.setLore(List.of(
                    "",
                    "§eBotão esquerdo: §fTeleportar",
                    "§eBotão direito: §fGerenciar",
                    "§eShift + direito: §fDeletar"
            ));
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }

        if (homes.isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§cNenhuma home encontrada");
            meta.setLore(List.of("§7Use §f/sethome <nome> §7para criar uma."));
            item.setItemMeta(meta);
            inventory.setItem(FIRST_HOME_SLOT, item);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§eVoltar");
        backMeta.setLore(List.of("§7Voltar para o menu de suas homes."));
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);

        player.openInventory(inventory);
    }

    private void openManage(Player player, String homeName) {
        Home home = service.getHome(player, homeName);
        if (home == null) {
            openHomes(player);
            return;
        }

        Inventory inventory = Bukkit.createInventory(new HomesHolder(HomesHolder.Type.MANAGE, home.name()), MANAGE_SIZE, "Gerenciar home");

        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = nameTag.getItemMeta();
        meta.setDisplayName("§aAlterar o nome");
        meta.setLore(List.of(
                "§7Atual: §f" + home.name(),
                "§aClique para alterar"
        ));
        nameTag.setItemMeta(meta);
        inventory.setItem(ALTER_NAME_SLOT, nameTag);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§eVoltar");
        backMeta.setLore(List.of("§7Voltar para suas homes."));
        back.setItemMeta(backMeta);
        inventory.setItem(MANAGE_BACK_SLOT, back);

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

        if (holder.type() == HomesHolder.Type.HOMES) {
            if (event.getRawSlot() == BACK_SLOT) {
                openIntro(player);
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.GRASS_BLOCK || clicked.getItemMeta() == null) return;

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            try {
                Home home = service.getHome(player, name);
                if (home == null || home.location().getWorld() == null) {
                    player.sendMessage("§cEsta home nao esta mais disponivel.");
                    openHomes(player);
                    return;
                }

                if (event.isShiftClick() && event.isRightClick()) {
                    if (service.deleteHome(player, name)) {
                        player.sendMessage("§aHome §f" + name + " §adeletada com sucesso.");
                    }
                    openHomes(player);
                    return;
                }

                if (event.isRightClick()) {
                    openManage(player, name);
                    return;
                }

                if (event.isLeftClick()) {
                    player.closeInventory();
                    player.teleport(home.location());
                    player.sendMessage("§aTeleportado para a home §f" + home.name() + "§a.");
                }
            } catch (IllegalArgumentException exception) {
                player.sendMessage("§cNao foi possivel carregar esta home.");
            }
            return;
        }

        if (holder.type() == HomesHolder.Type.MANAGE) {
            if (event.getRawSlot() == MANAGE_BACK_SLOT) {
                openHomes(player);
                return;
            }

            if (event.getRawSlot() == ALTER_NAME_SLOT) {
                String homeName = holder.homeName();
                if (homeName == null) return;
                pendingRenames.put(player.getUniqueId(), homeName);
                player.closeInventory();
                player.sendMessage("§eDigite no chat o novo nome da home §f" + homeName + "§e.");
                player.sendMessage("§7Digite §fcancelar §7para desistir.");
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof HomesHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String oldName = pendingRenames.get(event.getPlayer().getUniqueId());
        if (oldName == null) return;

        event.setCancelled(true);
        pendingRenames.remove(event.getPlayer().getUniqueId());

        String newName = event.getMessage().trim();
        Player player = event.getPlayer();
        if (newName.equalsIgnoreCase("cancelar")) {
            Bukkit.getScheduler().runTask(service.plugin(), () -> openManage(player, oldName));
            return;
        }

        Bukkit.getScheduler().runTask(service.plugin(), () -> {
            try {
                if (service.renameHome(player, oldName, newName)) {
                    player.sendMessage("§aHome renomeada de §f" + oldName + " §apara §f" + newName + "§a.");
                    openManage(player, newName);
                } else {
                    player.sendMessage("§cJa existe uma home com esse nome ou a home atual nao foi encontrada.");
                    openManage(player, oldName);
                }
            } catch (IllegalArgumentException exception) {
                player.sendMessage("§cNome invalido. Use apenas letras, numeros, _ ou -, com no maximo 32 caracteres.");
                openManage(player, oldName);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingRenames.remove(event.getPlayer().getUniqueId());
        service.unload(event.getPlayer());
    }

    private record HomesHolder(Type type, String homeName) implements InventoryHolder {
        private enum Type { INTRO, HOMES, MANAGE }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
