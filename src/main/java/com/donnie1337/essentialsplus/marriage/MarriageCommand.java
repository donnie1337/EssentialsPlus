package com.donnie1337.essentialsplus.marriage;

import com.donnie1337.essentialsplus.EssentialsPlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public final class MarriageCommand implements TabExecutor {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    private final EssentialsPlus plugin;
    private final MarriageService service;
    private final Map<UUID, Long> pendingDivorces = new HashMap<>();

    public MarriageCommand(EssentialsPlus plugin, MarriageService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color(message("jogador-apenas",
                    "&cEste comando precisa ser usado por um jogador.")));
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "aceitar", "accept" -> handleAccept(player);
            case "recusar", "deny" -> handleDeny(player);
            case "cancelar", "cancel" -> handleCancel(player);
            case "divorcio", "divórcio", "divorce" -> handleDivorce(player);
            case "partner", "parceiro" -> handlePartner(player);
            case "info" -> handleInfo(player);
            case "list", "lista" -> handleList(player);
            case "listpriests", "padres" -> handlePriests(player);
            case "ajuda", "help" -> {
                showHelp(player);
                yield true;
            }
            default -> handleRequest(player, args[0]);
        };
    }

    private boolean handleRequest(Player player, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(prefixed("jogador-nao-encontrado", "&cJogador não encontrado ou offline."));
            return true;
        }

        MarriageService.RequestResult result = service.request(player, target);
        switch (result) {
            case SELF -> player.sendMessage(prefixed("nao-pode-ser-voce",
                    "&cVocê não pode casar consigo mesmo."));
            case REQUESTER_MARRIED -> player.sendMessage(prefixed("voce-ja-casado",
                    "&cVocê já é casado."));
            case TARGET_MARRIED -> player.sendMessage(prefixed("alvo-ja-casado",
                    "&c{player} já é casado.").replace("{player}", target.getName()));
            case TARGET_DISABLED -> player.sendMessage(prefixed("alvo-nao-recebe-pedidos",
                    "&c{player} não está recebendo pedidos de casamento.")
                    .replace("{player}", target.getName()));
            case REQUESTER_HAS_PENDING -> player.sendMessage(prefixed("pedido-aberto-remetente",
                    "&eVocê já possui um pedido de casamento em aberto. Use &f/marry cancelar&e."));
            case TARGET_HAS_PENDING -> player.sendMessage(prefixed("pedido-aberto-alvo",
                    "&e{player} já possui um pedido de casamento pendente.")
                    .replace("{player}", target.getName()));
            case OK -> {
                player.sendMessage(prefixed("pedido-enviado",
                        "&aPedido de casamento enviado para &f{player}&a.")
                        .replace("{player}", target.getName()));
                target.sendMessage(prefixed("pedido-recebido",
                        "&f{player} &dquer se casar com você! &fUse &a/marry aceitar &fou &c/marry recusar&f.")
                        .replace("{player}", player.getName()));
            }
        }
        return true;
    }

    private boolean handleAccept(Player player) {
        MarriageService.AcceptResult result = service.accept(player);
        switch (result.status()) {
            case NO_REQUEST -> player.sendMessage(prefixed("sem-pedido",
                    "&eVocê não possui nenhum pedido de casamento pendente."));
            case REQUESTER_OFFLINE -> player.sendMessage(prefixed("remetente-offline",
                    "&cO jogador que enviou o pedido não está mais online."));
            case ALREADY_MARRIED -> player.sendMessage(prefixed("casamento-indisponivel",
                    "&cO casamento não pode mais ser concluído porque um dos jogadores já está casado."));
            case OK -> {
                MarriageService.Proposal proposal = result.proposal();
                Player partner = Bukkit.getPlayer(proposal.requesterId());
                player.sendMessage(prefixed("casamento-realizado",
                        "&aVocê agora está casado com &f{player}&a!")
                        .replace("{player}", proposal.requesterName()));
                if (partner != null) {
                    partner.sendMessage(prefixed("casamento-realizado",
                            "&aVocê agora está casado com &f{player}&a!")
                            .replace("{player}", player.getName()));
                }
                Bukkit.broadcastMessage(color(message("anuncio",
                        "&d❤ &f{player1} &de &f{player2} &dacabaram de se casar! &d❤"))
                        .replace("{player1}", proposal.requesterName())
                        .replace("{player2}", player.getName()));
            }
        }
        return true;
    }

    private boolean handleDeny(Player player) {
        MarriageService.Proposal proposal = service.deny(player.getUniqueId()).orElse(null);
        if (proposal == null) {
            player.sendMessage(prefixed("sem-pedido",
                    "&eVocê não possui nenhum pedido de casamento pendente."));
            return true;
        }

        player.sendMessage(prefixed("pedido-recusado",
                "&cVocê recusou o pedido de casamento de {player}.")
                .replace("{player}", proposal.requesterName()));
        Player requester = Bukkit.getPlayer(proposal.requesterId());
        if (requester != null) {
            requester.sendMessage(prefixed("pedido-recusado-remetente",
                    "&c{player} recusou seu pedido de casamento.")
                    .replace("{player}", player.getName()));
        }
        return true;
    }

    private boolean handleCancel(Player player) {
        MarriageService.Proposal proposal = service.cancelOutgoing(player.getUniqueId()).orElse(null);
        if (proposal == null) {
            player.sendMessage(prefixed("sem-pedido-enviado",
                    "&eVocê não possui nenhum pedido de casamento enviado."));
            return true;
        }

        player.sendMessage(prefixed("pedido-cancelado",
                "&aVocê cancelou o pedido de casamento para &f{player}&a.")
                .replace("{player}", proposal.targetName()));
        Player target = Bukkit.getPlayer(proposal.targetId());
        if (target != null) {
            target.sendMessage(prefixed("pedido-cancelado-alvo",
                    "&e{player} cancelou o pedido de casamento.")
                    .replace("{player}", player.getName()));
        }
        return true;
    }

    private boolean handleDivorce(Player player) {
        if (service.getMarriage(player.getUniqueId()).isEmpty()) {
            player.sendMessage(prefixed("nao-casado", "&eVocê não é casado."));
            return true;
        }

        if (confirmDivorce(player)) {
            long now = System.currentTimeMillis();
            Long expires = pendingDivorces.get(player.getUniqueId());
            if (expires == null || expires < now) {
                pendingDivorces.put(player.getUniqueId(), now + 10_000L);
                player.sendMessage(prefixed("confirmar-divorcio",
                        "&fDigite &c/marry divorcio &fnovamente em até &e10 segundos &fpara confirmar."));
                return true;
            }
            pendingDivorces.remove(player.getUniqueId());
        }

        MarriageService.Marriage marriage = service.divorce(player.getUniqueId()).orElse(null);
        if (marriage == null) {
            player.sendMessage(prefixed("nao-casado", "&eVocê não é casado."));
            return true;
        }

        player.sendMessage(prefixed("divorcio-realizado",
                "&aVocê se divorciou de &f{player}&a.")
                .replace("{player}", marriage.partnerName()));
        Player partner = Bukkit.getPlayer(marriage.partnerId());
        if (partner != null) {
            partner.sendMessage(prefixed("divorcio-parceiro",
                    "&e{player} se divorciou de você.")
                    .replace("{player}", player.getName()));
        }
        return true;
    }

    private boolean handlePartner(Player player) {
        MarriageService.Marriage marriage = service.getMarriage(player.getUniqueId()).orElse(null);
        if (marriage == null) {
            player.sendMessage(prefixed("nao-casado", "&eVocê não é casado."));
            return true;
        }
        player.sendMessage(prefixed("parceiro",
                "&fSeu parceiro é &d{player}&f.").replace("{player}", marriage.partnerName()));
        return true;
    }

    private boolean handleInfo(Player player) {
        MarriageService.Marriage marriage = service.getMarriage(player.getUniqueId()).orElse(null);
        if (marriage == null) {
            player.sendMessage(prefixed("nao-casado", "&eVocê não é casado."));
            return true;
        }

        player.sendMessage(color("&d[Marry] &fInformações do casamento:"));
        player.sendMessage(color("&8» &fParceiro: &d" + marriage.partnerName()));
        player.sendMessage(color("&8» &fCasados desde: &d" +
                DATE_FORMAT.format(Instant.ofEpochMilli(marriage.marriedAt()))));
        return true;
    }

    private boolean handleList(Player player) {
        List<MarriageService.Couple> couples = service.listCouples();
        if (couples.isEmpty()) {
            player.sendMessage(prefixed("nenhum-casal", "&eNão há jogadores casados."));
            return true;
        }

        player.sendMessage(color("&d[Marry] &fJogadores casados:"));
        for (MarriageService.Couple couple : couples) {
            player.sendMessage(color("&8» &d" + couple.firstName() + " &f❤ &d" + couple.secondName()));
        }
        return true;
    }

    private boolean handlePriests(Player player) {
        List<String> priests = service.onlinePriests();
        if (priests.isEmpty()) {
            player.sendMessage(prefixed("nenhum-padre", "&eNão há padres online."));
            return true;
        }
        player.sendMessage(color("&d[Marry] &fPadres online: &d" +
                String.join("&f, &d", priests)));
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(color("&d[Marry] &fComandos disponíveis para casamento:"));
        player.sendMessage(color("&8» &f/marry <player_name> &8- &bEnvia um pedido de casamento."));
        player.sendMessage(color("&8» &f/marry aceitar &8- &bAceita o pedido pendente."));
        player.sendMessage(color("&8» &f/marry recusar &8- &bRecusa o pedido pendente."));
        player.sendMessage(color("&8» &f/marry cancelar &8- &bCancela o pedido que você enviou."));
        player.sendMessage(color("&8» &f/marry divorcio &8- &bEncerra seu casamento atual."));
        player.sendMessage(color("&8» &f/marry partner &8- &bMostra seu parceiro."));
        player.sendMessage(color("&8» &f/marry info &8- &bMostra informações do casamento."));
        player.sendMessage(color("&8» &f/marry list &8- &bExibe todos os jogadores casados."));
        player.sendMessage(color("&8» &f/marry listpriests &8- &bExibe todos os padres online."));
        player.sendMessage(color("&8» &f/marry ajuda &8- &bExibe todos os comandos disponíveis."));
    }

    private boolean confirmDivorce(Player player) {
        org.bukkit.plugin.Plugin utilidades = Bukkit.getPluginManager().getPlugin("UtilidadesPlus");
        if (utilidades == null || !utilidades.isEnabled()) return true;
        try {
            Object result = utilidades.getClass()
                    .getMethod("confirmDivorce", Player.class)
                    .invoke(utilidades, player);
            return !(result instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    private String prefixed(String key, String fallback) {
        return color(message("prefix", "&d&lᴍᴀʀʀʏ &8• &r") + message(key, fallback));
    }

    private String message(String key, String fallback) {
        return plugin.getConfig().getString("messages.marry." + key, fallback);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();

        String input = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>(List.of(
                "aceitar", "recusar", "cancelar", "divorcio",
                "partner", "info", "list", "listpriests", "ajuda"
        ));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getName().equalsIgnoreCase(sender.getName())) {
                options.add(online.getName());
            }
        }

        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(input))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
