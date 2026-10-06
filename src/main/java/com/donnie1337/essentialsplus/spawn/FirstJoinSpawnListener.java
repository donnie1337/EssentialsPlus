package com.donnie1337.essentialsplus.spawn;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class FirstJoinSpawnListener implements Listener {

    private final SpawnService spawnService;
    private final FirstJoinService firstJoinService;

    public FirstJoinSpawnListener(SpawnService spawnService, FirstJoinService firstJoinService) {
        this.spawnService = spawnService;
        this.firstJoinService = firstJoinService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFirstJoin(PlayerJoinEvent event) {
        if (!firstJoinService.markAndCheckFirstJoin(event.getPlayer().getUniqueId())) {
            return;
        }

        Location spawn = spawnService.getSpawn().orElse(null);
        if (spawn == null) {
            return;
        }

        try {
            if (!spawn.getChunk().isLoaded()) {
                spawn.getChunk().load(true);
            }
            event.getPlayer().teleport(spawn);
        } catch (RuntimeException ignored) {
            // Se o destino não puder ser preparado neste momento,
            // o jogador entra normalmente em vez de interromper o join.
        }
    }
}
