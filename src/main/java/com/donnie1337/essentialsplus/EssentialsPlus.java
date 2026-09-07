package com.donnie1337.essentialsplus;

import com.donnie1337.essentialsplus.teleport.TeleportService;
import com.donnie1337.essentialsplus.teleport.command.TpaAcceptCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaCancelCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaDenyCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaHereCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EssentialsPlus extends JavaPlugin {

    private TeleportService teleportService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        teleportService = new TeleportService(this);
        teleportService.start();

        register("tpa", new TpaCommand(teleportService));
        register("tpahere", new TpaHereCommand(teleportService));
        register("tpaccept", new TpaAcceptCommand(teleportService));
        register("tpdeny", new TpaDenyCommand(teleportService));
        register("tpacancel", new TpaCancelCommand(teleportService));

        getLogger().info("EssentialsPlus habilitado com o sistema TPA.");
    }

    @Override
    public void onDisable() {
        if (teleportService != null) {
            teleportService.shutdown();
        }
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        final PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Comando não encontrado no plugin.yml: " + name);
        }
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }
}
