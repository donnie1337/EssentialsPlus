package com.donnie1337.essentialsplus;

import com.donnie1337.essentialsplus.auth.AuthSystemBridge;
import com.donnie1337.essentialsplus.bau.BauCommand;
import com.donnie1337.essentialsplus.bau.BauListener;
import com.donnie1337.essentialsplus.bau.BauService;
import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import com.donnie1337.essentialsplus.chat.TellCustomClickListener;
import com.donnie1337.essentialsplus.chat.TellListener;
import com.donnie1337.essentialsplus.command.CraftCommand;
import com.donnie1337.essentialsplus.command.EcCommand;
import com.donnie1337.essentialsplus.command.FlyCommand;
import com.donnie1337.essentialsplus.command.ReplyCommand;
import com.donnie1337.essentialsplus.command.TellCommand;
import com.donnie1337.essentialsplus.home.HomeGui;
import com.donnie1337.essentialsplus.home.HomeService;
import com.donnie1337.essentialsplus.home.command.DelHomeCommand;
import com.donnie1337.essentialsplus.home.command.HomeCommand;
import com.donnie1337.essentialsplus.home.command.HomesCommand;
import com.donnie1337.essentialsplus.home.command.SetHomeCommand;
import com.donnie1337.essentialsplus.inspect.InspectListener;
import com.donnie1337.essentialsplus.inspect.LiveInspectionService;
import com.donnie1337.essentialsplus.inspect.VerCommand;
import com.donnie1337.essentialsplus.teleport.StaffTeleportService;
import com.donnie1337.essentialsplus.teleport.TeleportService;
import com.donnie1337.essentialsplus.teleport.TpaCustomClickListener;
import com.donnie1337.essentialsplus.teleport.command.TpCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaAcceptCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaCancelCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaDenyCommand;
import com.donnie1337.essentialsplus.teleport.command.TpaHereCommand;
import com.donnie1337.essentialsplus.vanish.VanishCommand;
import com.donnie1337.essentialsplus.vanish.VanishListener;
import com.donnie1337.essentialsplus.vanish.VanishService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EssentialsPlus extends JavaPlugin {

    private TeleportService teleportService;
    private StaffTeleportService staffTeleportService;
    private HomeService homeService;
    private VanishService vanishService;
    private BauService bauService;
    private LiveInspectionService liveInspectionService;
    private BukkitTask bauAutoSaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        vanishService = new VanishService(this);
        getServer().getPluginManager().registerEvents(new VanishListener(vanishService), this);
        getServer().getPluginManager().registerEvents(new TellListener(this), this);
        new TellCustomClickListener(this).register(getServer().getPluginManager());
        register("v", new VanishCommand(vanishService));
        register("fly", new FlyCommand());
        register("craft", new CraftCommand());
        register("tell", new TellCommand());
        register("r", new ReplyCommand());

        bauService = new BauService(this);
        liveInspectionService = new LiveInspectionService(this, bauService);
        getServer().getPluginManager().registerEvents(new InspectListener(liveInspectionService), this);
        getServer().getPluginManager().registerEvents(new BauListener(bauService), this);

        register("ec", new EcCommand(liveInspectionService));
        register("ver", new VerCommand(liveInspectionService));
        register("bau", new BauCommand(bauService, liveInspectionService));

        startBauAutoSave();
        liveInspectionService.start();

        teleportService = new TeleportService(this, new ChatPlusBridge(), new AuthSystemBridge());
        teleportService.start();
        new TpaCustomClickListener(this, teleportService).register(getServer().getPluginManager());

        staffTeleportService = new StaffTeleportService(this);
        register("tp", new TpCommand(staffTeleportService));

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

        getLogger().info("EssentialsPlus habilitado com TPA, TP Staff, Homes, Vanish, Fly, Ender Chest, Bau, Ver, Craft, Tell e Reply.");
    }

    @Override
    public void onDisable() {
        if (liveInspectionService != null) liveInspectionService.stop();
        if (bauAutoSaveTask != null) bauAutoSaveTask.cancel();
        if (bauService != null) bauService.closeAllBaus();
        if (teleportService != null) teleportService.shutdown();
        if (vanishService != null) vanishService.clear();
    }

    private void startBauAutoSave() {
        bauAutoSaveTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (bauService != null) bauService.saveOpenBaus();
        }, 100L, 100L);
    }

    public boolean isVanished(Player player) {
        return vanishService != null && vanishService.isVanished(player);
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        final PluginCommand command = getCommand(name);
        if (command == null) throw new IllegalStateException("Comando não encontrado no plugin.yml: " + name);
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter completer) command.setTabCompleter(completer);
    }
}