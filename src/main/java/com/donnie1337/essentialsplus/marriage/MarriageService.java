package com.donnie1337.essentialsplus.marriage;

import com.donnie1337.essentialsplus.EssentialsPlus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class MarriageService {

    public record Marriage(UUID playerId, String playerName, UUID partnerId, String partnerName, long marriedAt) {}
    public record Proposal(UUID requesterId, String requesterName, UUID targetId, String targetName, long createdAt) {}
    public record Couple(String firstName, String secondName, long marriedAt) {}

    private final EssentialsPlus plugin;
    private final File file;
    private final Map<UUID, Marriage> marriages = new HashMap<>();
    private final Map<UUID, Proposal> proposalsByTarget = new HashMap<>();

    public MarriageService(EssentialsPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "marriages.yml");
        load();
    }

    public boolean isMarried(UUID playerId) {
        return marriages.containsKey(playerId);
    }

    public Optional<Marriage> getMarriage(UUID playerId) {
        return Optional.ofNullable(marriages.get(playerId));
    }

    public Optional<Proposal> getIncomingProposal(UUID targetId) {
        cleanupExpired();
        return Optional.ofNullable(proposalsByTarget.get(targetId));
    }

    public Optional<Proposal> getOutgoingProposal(UUID requesterId) {
        cleanupExpired();
        return proposalsByTarget.values().stream()
                .filter(proposal -> proposal.requesterId().equals(requesterId))
                .findFirst();
    }

    public RequestResult request(Player requester, Player target) {
        cleanupExpired();

        if (requester.getUniqueId().equals(target.getUniqueId())) return RequestResult.SELF;
        if (isMarried(requester.getUniqueId())) return RequestResult.REQUESTER_MARRIED;
        if (isMarried(target.getUniqueId())) return RequestResult.TARGET_MARRIED;
        if (!receivesMarriageRequests(target)) return RequestResult.TARGET_DISABLED;
        if (getOutgoingProposal(requester.getUniqueId()).isPresent()) return RequestResult.REQUESTER_HAS_PENDING;
        if (getIncomingProposal(target.getUniqueId()).isPresent()) return RequestResult.TARGET_HAS_PENDING;

        proposalsByTarget.put(target.getUniqueId(), new Proposal(
                requester.getUniqueId(), requester.getName(),
                target.getUniqueId(), target.getName(),
                System.currentTimeMillis()
        ));
        return RequestResult.OK;
    }

    public AcceptResult accept(Player target) {
        Proposal proposal = getIncomingProposal(target.getUniqueId()).orElse(null);
        if (proposal == null) return new AcceptResult(AcceptStatus.NO_REQUEST, null);

        Player requester = Bukkit.getPlayer(proposal.requesterId());
        if (requester == null || !requester.isOnline()) {
            proposalsByTarget.remove(target.getUniqueId());
            return new AcceptResult(AcceptStatus.REQUESTER_OFFLINE, proposal);
        }

        if (isMarried(target.getUniqueId()) || isMarried(requester.getUniqueId())) {
            proposalsByTarget.remove(target.getUniqueId());
            return new AcceptResult(AcceptStatus.ALREADY_MARRIED, proposal);
        }

        long now = Instant.now().toEpochMilli();
        marriages.put(requester.getUniqueId(), new Marriage(
                requester.getUniqueId(), requester.getName(),
                target.getUniqueId(), target.getName(), now
        ));
        marriages.put(target.getUniqueId(), new Marriage(
                target.getUniqueId(), target.getName(),
                requester.getUniqueId(), requester.getName(), now
        ));
        proposalsByTarget.remove(target.getUniqueId());
        save();
        return new AcceptResult(AcceptStatus.OK, proposal);
    }

    public Optional<Proposal> deny(UUID targetId) {
        cleanupExpired();
        return Optional.ofNullable(proposalsByTarget.remove(targetId));
    }

    public Optional<Proposal> cancelOutgoing(UUID requesterId) {
        cleanupExpired();
        Iterator<Map.Entry<UUID, Proposal>> iterator = proposalsByTarget.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Proposal> entry = iterator.next();
            if (entry.getValue().requesterId().equals(requesterId)) {
                Proposal proposal = entry.getValue();
                iterator.remove();
                return Optional.of(proposal);
            }
        }
        return Optional.empty();
    }

    public Optional<Marriage> divorce(UUID playerId) {
        Marriage marriage = marriages.remove(playerId);
        if (marriage == null) return Optional.empty();

        Marriage partnerMarriage = marriages.get(marriage.partnerId());
        if (partnerMarriage != null && partnerMarriage.partnerId().equals(playerId)) {
            marriages.remove(marriage.partnerId());
        }
        save();
        return Optional.of(marriage);
    }

    public List<Proposal> cancelRequestsFor(UUID playerId) {
        cleanupExpired();
        List<Proposal> cancelled = new ArrayList<>();
        Iterator<Map.Entry<UUID, Proposal>> iterator = proposalsByTarget.entrySet().iterator();
        while (iterator.hasNext()) {
            Proposal proposal = iterator.next().getValue();
            if (proposal.requesterId().equals(playerId) || proposal.targetId().equals(playerId)) {
                cancelled.add(proposal);
                iterator.remove();
            }
        }
        return cancelled;
    }

    public List<Couple> listCouples() {
        List<Couple> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Marriage marriage : marriages.values()) {
            String a = marriage.playerId().toString();
            String b = marriage.partnerId().toString();
            String key = a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
            if (!seen.add(key)) continue;

            result.add(new Couple(
                    safeName(marriage.playerId(), marriage.playerName()),
                    safeName(marriage.partnerId(), marriage.partnerName()),
                    marriage.marriedAt()
            ));
        }
        result.sort(Comparator.comparing(Couple::firstName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<String> onlinePriests() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("essentialsplus.marry.priest"))
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean receivesMarriageRequests(Player player) {
        org.bukkit.plugin.Plugin utilidades = Bukkit.getPluginManager().getPlugin("UtilidadesPlus");
        if (utilidades == null || !utilidades.isEnabled()) return true;
        try {
            Object result = utilidades.getClass()
                    .getMethod("receivesMarriageRequests", Player.class)
                    .invoke(utilidades, player);
            return !(result instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    private boolean receivesMarriageRequests(Player player) {
        org.bukkit.plugin.Plugin utilidades = Bukkit.getPluginManager().getPlugin("UtilidadesPlus");
        if (utilidades == null || !utilidades.isEnabled()) return true;
        try {
            Object result = utilidades.getClass()
                    .getMethod("receivesMarriageRequests", Player.class)
                    .invoke(utilidades, player);
            return !(result instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    private void cleanupExpired() {
        long timeoutMillis = Math.max(1L,
                plugin.getConfig().getLong("marriage.request-timeout-seconds", 60L)) * 1000L;
        long now = System.currentTimeMillis();
        proposalsByTarget.values().removeIf(proposal -> now - proposal.createdAt() > timeoutMillis);
    }

    private String safeName(UUID uuid, String fallback) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? fallback : player.getName();
    }

    private void load() {
        marriages.clear();
        if (!file.exists()) return;

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) return;

        for (String rawId : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(rawId);
                String partnerRaw = section.getString(rawId + ".partner");
                if (partnerRaw == null) continue;

                UUID partnerId = UUID.fromString(partnerRaw);
                marriages.put(playerId, new Marriage(
                        playerId,
                        section.getString(rawId + ".name", "Jogador"),
                        partnerId,
                        section.getString(rawId + ".partner-name", "Jogador"),
                        section.getLong(rawId + ".married-at", 0L)
                ));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Entrada de casamento inválida em marriages.yml: " + rawId);
            }
        }
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Marriage marriage : marriages.values()) {
            String path = "players." + marriage.playerId();
            data.set(path + ".name", marriage.playerName());
            data.set(path + ".partner", marriage.partnerId().toString());
            data.set(path + ".partner-name", marriage.partnerName());
            data.set(path + ".married-at", marriage.marriedAt());
        }

        try {
            file.getParentFile().mkdirs();
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar marriages.yml", exception);
        }
    }

    public enum RequestResult {
        OK, SELF, REQUESTER_MARRIED, TARGET_MARRIED, TARGET_DISABLED, REQUESTER_HAS_PENDING, TARGET_HAS_PENDING
    }

    public enum AcceptStatus {
        OK, NO_REQUEST, REQUESTER_OFFLINE, ALREADY_MARRIED
    }

    public record AcceptResult(AcceptStatus status, Proposal proposal) {}
}
