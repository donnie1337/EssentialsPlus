package com.donnie1337.essentialsplus.teleport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Representa uma ação temporária registrada para um botão de TPA. */
public record ButtonAction(int token, ButtonActionType type, UUID targetId, long createdAt) {
    private static final ConcurrentMap<Integer, ButtonAction> HISTORY = new ConcurrentHashMap<>();
    public ButtonAction(int token, ButtonActionType type, UUID targetId) { this(token, type, targetId, System.currentTimeMillis()); }
    public ButtonAction {
        if (type == null || targetId == null) throw new IllegalArgumentException("Tipo e alvo da ação do botão são obrigatórios.");
        HISTORY.put(token, this);
    }
    public static ButtonAction find(int token) { return HISTORY.get(token); }
    public static ButtonAction findRelated(int token, ButtonActionType type, UUID targetId) {
        ButtonAction source = HISTORY.get(token); if (source == null) return null;
        ButtonAction best = null;
        for (ButtonAction action : HISTORY.values()) {
            if (action.type() != type || !action.targetId().equals(targetId) || action.createdAt() < source.createdAt()) continue;
            if (best == null || action.createdAt() < best.createdAt()) best = action;
        }
        return best;
    }
    public static void removeIfExpired(long now, long timeoutMillis) {
        if (timeoutMillis <= 0) { HISTORY.clear(); return; }
        HISTORY.entrySet().removeIf(entry -> now - entry.getValue().createdAt() >= timeoutMillis);
    }
    public static void clear() { HISTORY.clear(); }
}
