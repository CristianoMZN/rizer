package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.IntegrationProvider;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class IntegrationDtos {

    public record IntegrationView(
        UUID id,
        IntegrationProvider provider,
        String status,
        String externalAccountId,
        String externalAccountName,
        boolean tokenValid,
        OffsetDateTime tokenExpiresAt,
        OffsetDateTime lastSyncAt,
        String lastError,
        List<String> scopes
    ) {}

    public record OAuthCallbackRequest(String code, String state) {}

    public record AuthorizeResponse(String authorizeUrl, String state) {}
}
