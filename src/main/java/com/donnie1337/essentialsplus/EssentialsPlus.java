package com.donnie1337.essentialsplus;

import com.donnie1337.essentialsplus.auth.AuthSystemBridge;
import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import com.donnie1337.essentialsplus.home.HomeGui;
import com.donnie1337.essentialsplus.home.HomeService;
import com.donnie1337.essentialsplus.home.command.DelHomeCommand;
import com.donnie1337.essentialsplus.home.command.HomeCommand;
import com.donnie1337.essentialsplus.home.command.HomesCommand;
import com.donnie1337.essentialsplus.home.command.SetHomeCommand;
import com.donnie1337.essentialsplus.teleport.TeleportService;
import com.donnie1337.essentialsplus.teleport.TpaCustomClickListener;
import com.donnie1337.essentialsplus.teleport.command.TpaAcceptCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaCancelCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaDenyCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaHereCommand;
import com.donnie1337.essentialsplus.vanish.VanishCommand;
import com.donnie1337.essentialsplus.vanish.VanishListener;
import com.donnie1337.essentialsplus.vanish.VanishService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EssentialsPlus extends JavaPlugin {

    private TeleportService teleportService;
    private HomeService homeService;
    private VanishService vanishService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        vanishService = new VanishService(this);
        getServer().getPluginManager().registerEvents(new VanishListener(vanishService), this);
        register("v", new VanishCommand(vanishService));

        teleportService = new TeleportService(this, new ChatPlusBridge(), new AuthSystemBridge());
        teleportService.start();
        new TpaCustomClickListener(this, teleportService).register(getServer().getPluginManager());

        homeService = new HomeService(this);
        HomeGui homeGui = new HomeGui(homeService);
        getServer().getPluginManager().registerEvents(homeGui, this);

        register("tpa", new TpaCommand(teleportService));
        register("tpaqui", new TpaHereCommand(teleportService));
        register("tpaccept", new TpaAcceptCommand(teleportService));
        register("tpdeny", new TpaDenyCommand(teleportService));
        register("tpacancel", new TpaCancelCommand(teleportService));

        register("home", new HomeCommand(homeService));
        register("homes", new HomesCommand(homeService, homeGui));
        register("sethome", new SetHomeCommand(homeService));
        register("delhome", new DelHomeCommand(homeService));

        getLogger().info("EssentialsPlus habilitado com TPA, Homes e Vanish.");
    }

    @Override
    public void onDisable() {
        if (teleportService != null) teleportService.shutdown();
        if (vanishService != null) vanishService.clear();
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        final PluginCommand command = getCommand(name);
        if (command == null) throw new IllegalStateException("Comando não encontrado no plugin.yml: " + name);
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter completer) command.setTabCompleter(completer);
    }
}
