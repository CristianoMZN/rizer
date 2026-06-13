package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MemberView(
    UUID id,
    UUID tenantId,
    String tenantSlug,
    String tenantName,
    UUID userId,
    String name,
    String email,
    TenantUserRole role,
    List<UUID> physicalStoreIds,
    boolean isActive,
    OffsetDateTime acceptedAt,
    OffsetDateTime expireAt
) {}
