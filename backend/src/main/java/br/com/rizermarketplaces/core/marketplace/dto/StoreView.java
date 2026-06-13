package br.com.rizermarketplaces.core.marketplace.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StoreView(
    UUID id,
    UUID tenantId,
    String name,
    String slug,
    String phone,
    String whatsapp,
    String email,
    boolean isMain,
    boolean isActive,
    Double latitude,
    Double longitude,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
