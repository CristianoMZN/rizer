package br.com.rizermarketplaces.core.marketplace.dto;

import java.util.List;
import java.util.Map;

public record PublicTenantView(
    String id,
    String slug,
    String tradeName,
    String legalName,
    String description,
    String logoUrl,
    String bannerUrl,
    String phone,
    String whatsapp,
    String email,
    String website,
    Map<String, Object> theme,
    List<PublicStoreView> stores,
    int activeProductsCount,
    List<String> realms
) {
    public record PublicStoreView(
        String id, String name, String slug, String phone, String whatsapp,
        String email, String city, String state, Double latitude, Double longitude,
        boolean isMain
    ) {}
}
