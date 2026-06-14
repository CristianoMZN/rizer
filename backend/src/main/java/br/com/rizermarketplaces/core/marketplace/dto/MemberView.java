package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberView(
    UUID id,
    UUID tenantId,
    String tenantSlug,
    String tenantName,
    UUID userId,
    String name,
    String email,
    String whatsapp,
    String avatarUrl,
    TenantUserRole role,
    List<UUID> physicalStoreIds,
    boolean isActive,
    boolean passwordMustChange,
    OffsetDateTime acceptedAt,
    OffsetDateTime expireAt
) {}
