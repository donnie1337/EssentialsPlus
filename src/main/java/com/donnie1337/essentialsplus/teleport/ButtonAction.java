package com.donnie1337.essentialsplus.teleport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Representa uma ação registrada para um botão de interação do TPA. */
public record ButtonAction(
        int token,
        ButtonActionType type,
        UUID targetId,
        long createdAt
) {
    private static final ConcurrentMap<Integer, ButtonAction> HISTORY = new ConcurrentHashMap<>();

    public ButtonAction(int token, ButtonActionType type, UUID targetId) {
        this(token, type, targetId, System.currentTimeMillis());
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
}
