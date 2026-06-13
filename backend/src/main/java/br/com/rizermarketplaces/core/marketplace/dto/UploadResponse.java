package br.com.rizermarketplaces.core.marketplace.dto;

import java.util.UUID;

public record UploadResponse(
    ProductView.ProductImageView image,
    String publicUrl,
    String s3Key
) {}
