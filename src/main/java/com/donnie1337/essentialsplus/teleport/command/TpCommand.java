package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.teleport.StaffTeleportService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TpCommand implements CommandExecutor {
    private final StaffTeleportService service;

    public TpCommand(StaffTeleportService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("essentialsplus.teleport")) {
            send(sender, "no-permission");
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                send(sender, "player-only");
                return true;
            }
            Player target = service.findPlayer(args[0]);
            if (target == null) {
                send(sender, "player-not-found");
                return true;
            }
            if (!service.teleport(player, target)) {
                send(sender, "teleport-failed");
                return true;
            }
            send(sender, "teleported-to", "player", target.getName());
            return true;
        }

        if (args.length == 2) {
            Player subject = service.findPlayer(args[0]);
            Player target = service.findPlayer(args[1]);
            if (subject == null || target == null) {
                send(sender, "player-not-found");
                return true;
            }
            if (!service.teleport(subject, target)) {
                send(sender, "teleport-failed");
                return true;
            }
            send(sender, "player-teleported-to", "player", subject.getName(), "target", target.getName());
            return true;
        }

        if (args.length == 3 || args.length == 4) {
            Player subject;
            int coordinateIndex;
            if (args.length == 3) {
                if (!(sender instanceof Player player)) {
                    send(sender, "player-only");
                    return true;
                }
                subject = player;
                coordinateIndex = 0;
            } else {
                subject = resolveSubject(sender, args[0]);
                coordinateIndex = 1;
                if (subject == null) {
                    send(sender, "player-not-found");
                    return true;
                }
            }

            Location origin = subject.getLocation();
            Double x = parseCoordinate(args[coordinateIndex], origin.getX());
            Double y = parseCoordinate(args[coordinateIndex + 1], origin.getY());
            Double z = parseCoordinate(args[coordinateIndex + 2], origin.getZ());
            if (x == null || y == null || z == null
                    || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || y < subject.getWorld().getMinHeight()
                    || y >= subject.getWorld().getMaxHeight()) {
                send(sender, "invalid-coordinates");
                return true;
            }

            Location destination = new Location(subject.getWorld(), x, y, z,
                    origin.getYaw(), origin.getPitch());
            if (!service.teleport(subject, destination)) {
                send(sender, "teleport-failed");
                return true;
            }

            send(sender, "teleported-to-coordinates",
                    "player", subject.getName(),
                    "x", format(x), "y", format(y), "z", format(z));
            return true;
        }

        send(sender, "usage-tp");
        return true;
    }

    private Player resolveSubject(CommandSender sender, String selector) {
        if ("@s".equalsIgnoreCase(selector)) {
            return sender instanceof Player player ? player : null;
        }
        return service.findPlayer(selector);
    }

    private Double parseCoordinate(String value, double origin) {
        if (value == null || value.isBlank() || value.startsWith("^")) return null;
        try {
            if (value.startsWith("~")) {
                String offset = value.substring(1);
                return origin + (offset.isBlank() ? 0.0D : Double.parseDouble(offset));
            }
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private void send(CommandSender sender, String key, String... replacements) {
        String raw = service.plugin().getConfig().getString("messages." + key, "");
        if ("usage-tp".equals(key)) {
            String prefix = service.plugin().getConfig().getString("messages.tp.prefix", "");
            raw = prefix + raw;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        sender.sendMessage(raw.replace('&', '§'));
    }
}
