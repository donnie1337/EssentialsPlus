package com.donnie1337.essentialsplus.home;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event.Result;
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
    private static final int MANAGE_SIZE = 45;
    private static final int INTRO_SLOT = 13;
    private static final int ALTER_NAME_SLOT = 20;
    private static final int TELEPORT_SLOT = 22;
    private static final int DELETE_SLOT = 24;
    private static final int MANAGE_BACK_SLOT = 40;
    private static final int BACK_SLOT = 49;
    private static final int[] HOME_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42};

    private final HomeService service;
    private final Map<UUID, String> pendingRenames = new HashMap<>();

    public HomeGui(HomeService service) {
        this.service = service;
    }

    public void openIntro(Player player) {
        Inventory inventory = Bukkit.createInventory(new HomesHolder(HomesHolder.Type.INTRO, null), INTRO_SIZE, "§8Homes");
        inventory.setItem(INTRO_SLOT, item(Material.COMPASS, "§aSuas homes", List.of(
                "§7Gerencie todos os pontos de",
                "§7teleporte personalizados.",
                "",
                "§eClique para abrir"
        )));
        player.openInventory(inventory);
    }

    public void openHomes(Player player) {
        Inventory inventory = Bukkit.createInventory(new HomesHolder(HomesHolder.Type.HOMES, null), HOMES_SIZE, "§8Homes §7→ §fSuas Homes");

        Map<String, Home> homes = service.homes(player);
        int index = 0;
        for (Home home : homes.values()) {
            if (index >= HOME_SLOTS.length) break;
            inventory.setItem(HOME_SLOTS[index++], item(worldMaterial(home), "§a" + home.name(), List.of(
                    "",
                    "§7Mundo: §f" + worldName(home),
                    "§7Localização: §f" + coordinates(home),
                    "",
                    "§7Clique esquerdo §f→ §eTeleportar",
                    "§7Clique direito §f→ §eGerenciar",
                    "§7Shift + direito §f→ §cDeletar"
            )));
        }

        if (homes.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, "§cNenhuma home encontrada", List.of(
                    "§7Use §f/sethome <nome> §7para criar uma."
            )));
        }

        inventory.setItem(BACK_SLOT, item(Material.ARROW, "§eVoltar", List.of("§7Voltar para o menu de homes.")));
        player.openInventory(inventory);
    }

    private void openManage(Player player, String homeName) {
        Home home = service.getHome(player, homeName);
        if (home == null) {
            openHomes(player);
            return;
        }

        Inventory inventory = Bukkit.createInventory(new HomesHolder(HomesHolder.Type.MANAGE, home.name()), MANAGE_SIZE, "§8Gerenciar §7→ §f" + home.name());

        inventory.setItem(13, item(worldMaterial(home), "§a" + home.name(), List.of(
                "§7Sua localização salva",
                "",
                "§7Mundo: §f" + worldName(home),
                "§7X: §f" + home.location().getBlockX(),
                "§7Y: §f" + home.location().getBlockY(),
                "§7Z: §f" + home.location().getBlockZ(),
                "",
                "§8Escolha uma ação abaixo"
        )));

        inventory.setItem(20, item(Material.NAME_TAG, "§eRenomear Home", List.of(
                "§7Altere o nome desta home.",
                "",
                "§7Nome atual: §f" + home.name(),
                "",
                "§aClique para renomear"
        )));

        inventory.setItem(22, item(Material.ENDER_PEARL, "§bTeleportar", List.of(
                "§7Teleporte diretamente para esta home.",
                "",
                "§7Destino: §f" + worldName(home),
                "§7Localização: §f" + coordinates(home),
                "",
                "§aClique para teleportar"
        )));

        inventory.setItem(24, item(Material.BARRIER, "§cDeletar Home", List.of(
                "§7Remove permanentemente esta home.",
                "",
                "§7Home: §f" + home.name(),
                "",
                "§cClique para deletar"
        )));

        inventory.setItem(MANAGE_BACK_SLOT, item(Material.ARROW, "§eVoltar", List.of("§7Voltar para suas homes.")));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof HomesHolder holder)) return;

        // O inventário do jogador também fica protegido enquanto qualquer menu de Homes estiver aberto.
        event.setCancelled(true);
        event.setResult(Result.DENY);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            player.updateInventory();
            return;
        }

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
            if (clicked == null || !isHomeMaterial(clicked.getType()) || clicked.getItemMeta() == null) {
                player.updateInventory();
                return;
            }

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
                    player.closeInventory();
                    return;
                }

                // Shift + clique esquerdo nunca deve mover o item para o inventário do jogador.
                if (event.isShiftClick()) {
                    player.updateInventory();
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
                player.updateInventory();
            }
            return;
        }

        if (holder.type() == HomesHolder.Type.MANAGE) {
            String homeName = holder.homeName();
            if (homeName == null) {
                player.updateInventory();
                return;
            }

            if (event.getRawSlot() == MANAGE_BACK_SLOT) {
                openHomes(player);
                return;
            }

            if (event.getRawSlot() == ALTER_NAME_SLOT) {
                pendingRenames.put(player.getUniqueId(), homeName);
                player.closeInventory();
                player.sendMessage("§eDigite no chat o novo nome da home §f" + homeName + "§e.");
                player.sendMessage("§7Digite §fcancelar §7para desistir.");
                return;
            }

            if (event.getRawSlot() == TELEPORT_SLOT) {
                Home home = service.getHome(player, homeName);
                if (home == null) {
                    openHomes(player);
                    return;
                }
                player.closeInventory();
                player.teleport(home.location());
                player.sendMessage("§aTeleportado para a home §f" + home.name() + "§a.");
                return;
            }

            if (event.getRawSlot() == DELETE_SLOT) {
                if (service.deleteHome(player, homeName)) {
                    player.sendMessage("§aHome §f" + homeName + " §adeletada com sucesso.");
                }
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof HomesHolder) {
            event.setCancelled(true);
            event.setResult(Result.DENY);
            if (event.getWhoClicked() instanceof Player player) player.updateInventory();
        }
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
                player.sendMessage("§cNome invalido. Use apenas letras, numeros, _ ou -, com no maximo 16 caracteres.");
                openManage(player, oldName);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingRenames.remove(event.getPlayer().getUniqueId());
        service.unload(event.getPlayer());
    }

    private static Material worldMaterial(Home home) {
        World world = home.location().getWorld();
        if (world == null) return Material.LODESTONE;
        return switch (world.getEnvironment()) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.LODESTONE;
        };
    }

    private static boolean isHomeMaterial(Material material) {
        return material == Material.LODESTONE || material == Material.NETHERRACK || material == Material.END_STONE;
    }

    private static String worldName(Home home) {
        World world = home.location().getWorld();
        return world == null ? "Indisponível" : world.getName();
    }

    private static String coordinates(Home home) {
        return home.location().getBlockX() + ", " + home.location().getBlockY() + ", " + home.location().getBlockZ();
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private record HomesHolder(Type type, String homeName) implements InventoryHolder {
        private enum Type { INTRO, HOMES, MANAGE }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
