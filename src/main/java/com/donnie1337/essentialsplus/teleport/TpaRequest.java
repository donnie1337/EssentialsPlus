package com.donnie1337.essentialsplus.teleport;

import java.util.UUID;

public record TpaRequest(
        UUID requesterId,
        String requesterName,
        UUID recipientId,
        String recipientName,
        boolean here,
        long createdAt
) {
    public TpaRequest {
        if (requesterId == null || recipientId == null) {
            throw new IllegalArgumentException("Dados obrigatórios da solicitação não podem ser nulos.");
        }
    }

    public boolean isExpired(long now, long timeoutMillis) {
        return timeoutMillis > 0 && now - createdAt >= timeoutMillis;
    }
}
