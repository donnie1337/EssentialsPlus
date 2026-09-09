package com.donnie1337.essentialsplus.teleport;

import java.util.UUID;

public record ButtonAction(
        int token,
        ButtonActionType type,
        UUID targetId
) {
    public ButtonAction {
        if (type == null || targetId == null) {
            throw new IllegalArgumentException("Tipo e alvo da ação do botão são obrigatórios.");
        }
    }
}
