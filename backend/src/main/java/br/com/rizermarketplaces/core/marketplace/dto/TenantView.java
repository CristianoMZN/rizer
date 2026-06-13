package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.CustomDomainStatus;
import br.com.rizermarketplaces.core.marketplace.model.TenantStatus;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TenantView(
    UUID id,
    String slug,
    String countryCode,
    String tradeName,
    String legalName,
    String cnpj,
    String description,
    String logoUrl,
    String bannerUrl,
    String phone,
    String whatsapp,
    String email,
    String website,
    TenantStatus status,
    boolean isPublic,
    boolean isPartnerPageEnabled,
    boolean hadTrial,
    String customDomain,
    CustomDomainStatus customDomainStatus,
    String ownerUserId,
    String ownerEmail,
    String ownerName,
    long activeStoresCount,
    long membersCount,
    List<StoreView> stores,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public record StoreView(
        UUID id, String name, String slug, String phone, String whatsapp,
        boolean isMain, boolean isActive
    ) {}
}
