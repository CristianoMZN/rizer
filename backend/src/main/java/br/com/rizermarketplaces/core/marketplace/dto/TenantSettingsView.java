package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.CustomDomainStatus;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.TenantStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TenantSettingsView(
    UUID id,
    String slug,
    String countryCode,
    String tradeName,
    String legalName,
    String description,
    String phone,
    String whatsapp,
    String email,
    String website,
    String logoUrl,
    String bannerUrl,
    String cnpj,
    TenantStatus status,
    boolean isPublic,
    boolean isPartnerPageEnabled,
    boolean hadTrial,
    String customDomain,
    CustomDomainStatus customDomainStatus,
    String customDomainError,
    OffsetDateTime customDomainLastCheckAt,
    Map<String, Object> theme
) {
    public static TenantSettingsView of(Tenant t) {
        return new TenantSettingsView(
            t.getId(), t.getSlug(), t.getCountryCode(), t.getTradeName(),
            t.getLegalName(), t.getDescription(), t.getPhone(), t.getWhatsapp(),
            t.getEmail(), t.getWebsite(), t.getLogoUrl(), t.getBannerUrl(), t.getCnpj(),
            t.getStatus(), t.isPublic(), t.isPartnerPageEnabled(), t.isHadTrial(),
            t.getCustomDomain(), t.getCustomDomainStatus(), t.getCustomDomainError(),
            t.getCustomDomainLastCheckAt(), t.getTheme()
        );
    }
}
