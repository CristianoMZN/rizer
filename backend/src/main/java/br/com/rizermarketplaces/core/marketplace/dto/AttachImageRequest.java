package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachImageRequest(
    @NotNull UUID productId,
    @NotNull String s3Key,
    @NotNull String s3Bucket,
    @NotNull String publicUrl,
    String contentType,
    Integer width,
    Integer height,
    Integer sortOrder,
    Boolean isCover
) {}
