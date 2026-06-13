package br.com.rizermarketplaces.core.marketplace.dto;

import java.util.List;

public record PublicPartnerView(
    String id,
    String slug,
    String tradeName,
    String description,
    String logoUrl,
    String bannerUrl,
    String website,
    List<PublicStoreSummary> stores,
    int activeProductsCount,
    List<String> realms
) {
    public record PublicStoreSummary(
        String id, String name, String slug, String city, String state, boolean isMain
    ) {}
}
