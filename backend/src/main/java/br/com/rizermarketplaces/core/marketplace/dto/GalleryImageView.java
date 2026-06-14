package br.com.rizermarketplaces.core.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GalleryImageView(
    UUID id,
    String url,
    String caption,
    short sortOrder,
    boolean isCover,
    OffsetDateTime createdAt
) {}
