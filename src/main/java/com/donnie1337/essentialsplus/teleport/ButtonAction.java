package com.donnie1337.essentialsplus.teleport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Representa uma ação temporária registrada para um botão de TPA. */
public record ButtonAction(int token, ButtonActionType type, UUID ownerId, UUID targetId, long createdAt) {
    private static final ConcurrentMap<Integer, ButtonAction> HISTORY = new ConcurrentHashMap<>();

    public ButtonAction(int token, ButtonActionType type, UUID targetId) {
        this(token, type, null, targetId, System.currentTimeMillis());
    }

    public ButtonAction(int token, ButtonActionType type, UUID ownerId, UUID targetId) {
        this(token, type, ownerId, targetId, System.currentTimeMillis());
    }

    public ButtonAction {
        if (type == null || targetId == null) {
            throw new IllegalArgumentException("Tipo e alvo da ação do botão são obrigatórios.");
        }
        HISTORY.put(token, this);
    }

    public static ButtonAction find(int token) {
        return HISTORY.get(token);
    }

    public static java.util.List<ButtonAction> findRelated(ButtonAction source) {
        if (source == null) return java.util.List.of();
        java.util.List<ButtonAction> related = new java.util.ArrayList<>();
        for (ButtonAction action : HISTORY.values()) {
            if (action.token() == source.token()) continue;
            if (action.ownerId() == null || source.ownerId() == null) continue;
            boolean sameRequest = source.ownerId().equals(action.ownerId())
                    && source.targetId().equals(action.targetId());
            boolean oppositeSide = source.ownerId().equals(action.targetId())
                    && source.targetId().equals(action.ownerId());
            if (sameRequest || oppositeSide) related.add(action);
        }
        return related;
    }

    public static void removeIfExpired(long now, long timeoutMillis) {
        if (timeoutMillis <= 0) {
            HISTORY.clear();
            return;
        }
        HISTORY.entrySet().removeIf(entry -> now - entry.getValue().createdAt() >= timeoutMillis);
    }

    public static void clear() {
        HISTORY.clear();
    }
}
