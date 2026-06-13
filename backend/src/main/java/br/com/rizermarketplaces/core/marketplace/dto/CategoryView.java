package br.com.rizermarketplaces.core.marketplace.dto;

import java.util.UUID;

public record CategoryView(
    UUID id,
    String countryCode,
    String realm,
    String path,
    String name,
    String slug,
    UUID parentId,
    short level,
    int sortOrder,
    String icon,
    String imageUrl,
    String description
) {}
