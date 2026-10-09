package com.donnie1337.essentialsplus.flight;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FlightService implements Listener {
    private final JavaPlugin plugin;
    private final Set<UUID> gliding = new HashSet<>();
    private final Set<UUID> terrainFlight = new HashSet<>();
    private final Set<UUID> insideOwnTerrain = new HashSet<>();
    private Object terrenosManager;
    private Method terrenosFindMethod;
    private Method terrenoOwnerIdMethod;

    public FlightService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canFly(Player player) {
        return player != null && player.hasPermission("essentialsplus.fly");
    }

    public boolean isFlying(Player player) {
        return player != null && player.getAllowFlight() && player.isFlying();
    }

    public boolean toggle(Player player) {
        if (player == null) return false;

        // Administrador ou superior pode usar /fly em qualquer lugar.
        // Ajudante/moderador só podem usar em terrenos; jogadores não-staff
        // com a permissão ficam limitados ao próprio terreno.
        String cargo = getCargoGroup(player);
        boolean adminOrHigher = isAdminOrHigher(cargo);
        boolean staff = isStaffCargo(cargo);
        boolean allowedHere = adminOrHigher
                || (staff ? isInsideAnyTerrain(player) : isInsideOwnTerrain(player));

        if (!allowedHere) {
            if (terrainFlight.contains(player.getUniqueId())) {
                disableTerrainFlight(player);
            }
            return false;
        }

        if (player.getAllowFlight()) {
            disableTerrainFlight(player);
            return false;
        }

        enableTerrainFlight(player);
        return true;
    }

    public void enable(Player player) {
        if (player == null) return;
        gliding.remove(player.getUniqueId());
        player.setFallDistance(0.0F);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void disable(Player player) {
        if (player == null) return;

        player.setFlying(false);
        player.setAllowFlight(false);
        player.setFallDistance(0.0F);

        if (!player.isOnGround() && !isInFluid(player)) {
            // Mantém a queda vanilla para preservar totalmente o controle
            // horizontal do jogador. O marcador "gliding" serve apenas para
            // zerar dano de queda até ele tocar o chão.
            gliding.add(player.getUniqueId());
        } else {
            gliding.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> syncTerrainFlight(player));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getTo() != null
                && (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                || event.getFrom().getWorld() != event.getTo().getWorld())) {
            syncTerrainFlight(player);
        }

        if (!gliding.contains(uuid)) return;

        if (player.isOnGround() || isInFluid(player)) {
            stopGlide(player);
            return;
        }

        // Não alteramos a velocidade do jogador enquanto ele cai. Assim,
        // W/A/S/D continuam com o comportamento normal do Minecraft.
        player.setFallDistance(0.0F);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getCause() == EntityDamageEvent.DamageCause.FALL
                && gliding.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        gliding.remove(uuid);
        terrainFlight.remove(uuid);
        insideOwnTerrain.remove(uuid);
    }

    private void syncTerrainFlight(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        String cargo = getCargoGroup(player);
        boolean staff = isStaffCargo(cargo);
        boolean adminOrHigher = isAdminOrHigher(cargo);
        boolean insideAllowedTerrain = canFly(player)
                && (staff ? isInsideAnyTerrain(player) : isInsideOwnTerrain(player));
        boolean wasInside = insideOwnTerrain.contains(uuid);

        if (insideAllowedTerrain) {
            insideOwnTerrain.add(uuid);
            if (!wasInside || !terrainFlight.contains(uuid)) {
                enableTerrainFlight(player);
                player.sendMessage("§a[Voo] §rModo de voo ativado.");
            }
            return;
        }

        insideOwnTerrain.remove(uuid);
        if (terrainFlight.contains(uuid)) {
            if (adminOrHigher) {
                // Administrador, Gerente e DEV podem continuar voando fora de terrenos.
                terrainFlight.remove(uuid);
            } else {
                disableTerrainFlight(player);
                if (canFly(player)) {
                    player.sendMessage("§c[Voo] §rModo de voo desativado.");
                }
            }
        }
    }

    private void enableTerrainFlight(Player player) {
        if (player == null || !canFly(player)) return;
        terrainFlight.add(player.getUniqueId());
        enable(player);
    }

    private void disableTerrainFlight(Player player) {
        if (player == null) return;
        terrainFlight.remove(player.getUniqueId());

        // Criativo e espectador possuem voo nativo; não retiramos esse estado.
        switch (player.getGameMode()) {
            case CREATIVE, SPECTATOR -> {
                gliding.remove(player.getUniqueId());
                player.setFallDistance(0.0F);
            }
            default -> disable(player);
        }
    }

    private String getCargoGroup(Player player) {
        if (player == null || !player.isOnline()) return "";

        try {
            var cargoPlus = plugin.getServer().getPluginManager().getPlugin("CargoPlus");
            if (cargoPlus == null || !cargoPlus.isEnabled()) return "";

            Method apiMethod = cargoPlus.getClass().getMethod("api");
            Object api = apiMethod.invoke(cargoPlus);
            if (api == null) return "";

            Method getGroup = api.getClass().getMethod("getGroup", UUID.class);
            Object value = getGroup.invoke(api, player.getUniqueId());
            return value == null
                    ? ""
                    : String.valueOf(value).trim().toLowerCase(java.util.Locale.ROOT);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return "";
        }
    }

    private boolean isStaffCargo(String group) {
        return switch (group) {
            case "ajudante", "moderador", "administrador", "gerente", "dev" -> true;
            default -> false;
        };
    }

    private boolean isAdminOrHigher(String group) {
        return switch (group) {
            case "administrador", "gerente", "dev" -> true;
            default -> false;
        };
    }

    private boolean isInsideAnyTerrain(Player player) {
        if (player == null || !player.isOnline()) return false;

        try {
            var terrenosPlus = plugin.getServer().getPluginManager().getPlugin("TerrenosPlus");
            if (terrenosPlus == null || !terrenosPlus.isEnabled()) {
                clearTerrenosBridge();
                return false;
            }

            if (terrenosManager == null || terrenosFindMethod == null) {
                Method getManager = terrenosPlus.getClass().getMethod("getTerrenoManager");
                terrenosManager = getManager.invoke(terrenosPlus);
                if (terrenosManager == null) return false;
                terrenosFindMethod = terrenosManager.getClass().getMethod("find", Location.class);
            }

            Object result = terrenosFindMethod.invoke(terrenosManager, player.getLocation());
            return result instanceof Optional<?> optional && optional.isPresent();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            clearTerrenosBridge();
            return false;
        }
    }

    private boolean isInsideOwnTerrain(Player player) {
        if (player == null || !player.isOnline()) return false;

        try {
            var terrenosPlus = plugin.getServer().getPluginManager().getPlugin("TerrenosPlus");
            if (terrenosPlus == null || !terrenosPlus.isEnabled()) {
                clearTerrenosBridge();
                return false;
            }

            if (terrenosManager == null || terrenosFindMethod == null) {
                Method getManager = terrenosPlus.getClass().getMethod("getTerrenoManager");
                terrenosManager = getManager.invoke(terrenosPlus);
                if (terrenosManager == null) return false;
                terrenosFindMethod = terrenosManager.getClass().getMethod("find", Location.class);
            }

            Object result = terrenosFindMethod.invoke(terrenosManager, player.getLocation());
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) return false;

            Object terrain = optional.get();
            if (terrenoOwnerIdMethod == null
                    || !terrenoOwnerIdMethod.getDeclaringClass().isAssignableFrom(terrain.getClass())) {
                terrenoOwnerIdMethod = terrain.getClass().getMethod("ownerId");
            }

            Object owner = terrenoOwnerIdMethod.invoke(terrain);
            return player.getUniqueId().equals(owner);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            clearTerrenosBridge();
            return false;
        }
    }

    private void clearTerrenosBridge() {
        terrenosManager = null;
        terrenosFindMethod = null;
        terrenoOwnerIdMethod = null;
    }

    private boolean isInFluid(Player player) {
        Material feet = player.getLocation().getBlock().getType();
        Material head = player.getEyeLocation().getBlock().getType();
        return feet == Material.WATER || feet == Material.LAVA
                || head == Material.WATER || head == Material.LAVA;
    }

    private void stopGlide(Player player) {
        gliding.remove(player.getUniqueId());
        player.setFallDistance(0.0F);
    }
}
