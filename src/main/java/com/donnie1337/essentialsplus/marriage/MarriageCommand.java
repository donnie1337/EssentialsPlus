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

public final class MarriageCommand implements TabExecutor {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    private final EssentialsPlus plugin;
    private final MarriageService service;

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
            case "casar", "marry" -> handleRequest(player, args);
            case "aceitar", "accept" -> handleAccept(player);
            case "recusar", "deny" -> handleDeny(player);
            case "partner", "parceiro" -> handlePartner(player);
            case "info" -> handleInfo(player);
            case "list", "lista" -> handleList(player);
            case "listpriests", "padres" -> handlePriests(player);
            case "ajuda", "help" -> {
                showHelp(player);
                yield true;
            }
            default -> {
                showHelp(player);
                yield true;
            }
        };
    }

    private boolean handleRequest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(prefixed("uso-casar", "&fUse: &d/marry casar <jogador>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
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
            case ALREADY_PENDING -> player.sendMessage(prefixed("pedido-ja-enviado",
                    "&eVocê já enviou um pedido de casamento para {player}.").replace("{player}", target.getName()));
            case OK -> {
                player.sendMessage(prefixed("pedido-enviado",
                        "&aPedido de casamento enviado para &f{player}&a.").replace("{player}", target.getName()));
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
                        "&aVocê agora está casado com &f{player}&a!").replace("{player}", proposal.requesterName()));
                if (partner != null) {
                    partner.sendMessage(prefixed("casamento-realizado",
                            "&aVocê agora está casado com &f{player}&a!").replace("{player}", player.getName()));
                }
                String broadcast = color(message("anuncio",
                        "&d❤ &f{player1} &de &f{player2} &dacabaraм de se casar! &d❤"))
                        .replace("{player1}", proposal.requesterName())
                        .replace("{player2}", player.getName());
                Bukkit.broadcastMessage(broadcast);
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
                "&cVocê recusou o pedido de casamento de {player}.").replace("{player}", proposal.requesterName()));
        Player requester = Bukkit.getPlayer(proposal.requesterId());
        if (requester != null) {
            requester.sendMessage(prefixed("pedido-recusado-remetente",
                    "&c{player} recusou seu pedido de casamento.").replace("{player}", player.getName()));
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
        player.sendMessage(color("&d[Marry] &fPadres online: &d" + String.join("&f, &d", priests)));
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(color("&d[Marry] &fComandos disponíveis para casamento:"));
        player.sendMessage(color("&8» &f/marry list &8- &bExibe todos os jogadores casados."));
        player.sendMessage(color("&8» &f/marry listpriests &8- &bExibe todos os padres online."));
        player.sendMessage(color("&8» &f/marry partner &8- &bLista o parceiro de um player."));
        player.sendMessage(color("&8» &f/marry casar <player_name> &8- &bEnvia um pedido de casamento para outro jogador."));
        player.sendMessage(color("&8» &f/marry aceitar &8- &bAceita o pedido de casamento pendente."));
        player.sendMessage(color("&8» &f/marry recusar &8- &bRecusa o pedido de casamento pendente."));
        player.sendMessage(color("&8» &f/marry info &8- &bMostra informações sobre o casamento."));
        player.sendMessage(color("&8» &f/marry ajuda &8- &bExibe todos os comandos disponíveis."));
    }

    private String prefixed(String key, String fallback) {
        String prefix = message("prefix", "&d&lᴍᴀʀʀʏ &8• &r");
        return color(prefix + message(key, fallback));
    }

    private String message(String key, String fallback) {
        return plugin.getConfig().getString("messages.marry." + key, fallback);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = List.of(
                    "casar", "aceitar", "recusar", "partner", "info", "list", "listpriests", "ajuda"
            );
            String input = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(option -> option.startsWith(input)).toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("casar") || args[0].equalsIgnoreCase("marry"))) {
            String input = args[1].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getName().equalsIgnoreCase(sender.getName())
                        && online.getName().toLowerCase(Locale.ROOT).startsWith(input)) {
                    names.add(online.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
