package com.donnie1337.essentialsplus.marriage;

import com.donnie1337.essentialsplus.EssentialsPlus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class MarriageService {

    public record Marriage(UUID playerId, String playerName, UUID partnerId, String partnerName, long marriedAt) {}
    public record Proposal(UUID requesterId, String requesterName, long createdAt) {}
    public record Couple(String firstName, String secondName, long marriedAt) {}

    private final EssentialsPlus plugin;
    private final File file;
    private final Map<UUID, Marriage> marriages = new HashMap<>();
    private final Map<UUID, Proposal> proposals = new HashMap<>();

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

    public Optional<Proposal> getPendingProposal(UUID targetId) {
        Proposal proposal = proposals.get(targetId);
        if (proposal == null) return Optional.empty();

        long timeoutMillis = Math.max(1L,
                plugin.getConfig().getLong("marriage.request-timeout-seconds", 60L)) * 1000L;
        if (System.currentTimeMillis() - proposal.createdAt() > timeoutMillis) {
            proposals.remove(targetId);
            return Optional.empty();
        }
        return Optional.of(proposal);
    }

    public RequestResult request(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) return RequestResult.SELF;
        if (isMarried(requester.getUniqueId())) return RequestResult.REQUESTER_MARRIED;
        if (isMarried(target.getUniqueId())) return RequestResult.TARGET_MARRIED;

        Proposal existing = getPendingProposal(target.getUniqueId()).orElse(null);
        if (existing != null && existing.requesterId().equals(requester.getUniqueId())) {
            return RequestResult.ALREADY_PENDING;
        }

        proposals.put(target.getUniqueId(),
                new Proposal(requester.getUniqueId(), requester.getName(), System.currentTimeMillis()));
        return RequestResult.OK;
    }

    public AcceptResult accept(Player target) {
        Proposal proposal = getPendingProposal(target.getUniqueId()).orElse(null);
        if (proposal == null) return new AcceptResult(AcceptStatus.NO_REQUEST, null);

        Player requester = Bukkit.getPlayer(proposal.requesterId());
        if (requester == null || !requester.isOnline()) {
            proposals.remove(target.getUniqueId());
            return new AcceptResult(AcceptStatus.REQUESTER_OFFLINE, proposal);
        }

        if (isMarried(target.getUniqueId()) || isMarried(requester.getUniqueId())) {
            proposals.remove(target.getUniqueId());
            return new AcceptResult(AcceptStatus.ALREADY_MARRIED, proposal);
        }

        long now = Instant.now().toEpochMilli();
        Marriage first = new Marriage(
                requester.getUniqueId(), requester.getName(),
                target.getUniqueId(), target.getName(), now
        );
        Marriage second = new Marriage(
                target.getUniqueId(), target.getName(),
                requester.getUniqueId(), requester.getName(), now
        );
        marriages.put(requester.getUniqueId(), first);
        marriages.put(target.getUniqueId(), second);
        proposals.remove(target.getUniqueId());
        save();
        return new AcceptResult(AcceptStatus.OK, proposal);
    }

    public Optional<Proposal> deny(UUID targetId) {
        Proposal proposal = getPendingProposal(targetId).orElse(null);
        if (proposal != null) proposals.remove(targetId);
        return Optional.ofNullable(proposal);
    }

    public List<Couple> listCouples() {
        List<Couple> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Marriage marriage : marriages.values()) {
            String a = marriage.playerId().toString();
            String b = marriage.partnerId().toString();
            String key = a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
            if (!seen.add(key)) continue;

            String first = safeName(marriage.playerId(), marriage.playerName());
            String second = safeName(marriage.partnerId(), marriage.partnerName());
            result.add(new Couple(first, second, marriage.marriedAt()));
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
                String playerName = section.getString(rawId + ".name", "Jogador");
                String partnerName = section.getString(rawId + ".partner-name", "Jogador");
                long marriedAt = section.getLong(rawId + ".married-at", 0L);
                marriages.put(playerId,
                        new Marriage(playerId, playerName, partnerId, partnerName, marriedAt));
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
        OK, SELF, REQUESTER_MARRIED, TARGET_MARRIED, ALREADY_PENDING
    }

    public enum AcceptStatus {
        OK, NO_REQUEST, REQUESTER_OFFLINE, ALREADY_MARRIED
    }

    public record AcceptResult(AcceptStatus status, Proposal proposal) {}
}
