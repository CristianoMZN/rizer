package br.com.rizermarketplaces.core.marketplace.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductView(
    UUID id,
    UUID tenantId,
    UUID physicalStoreId,
    String physicalStoreName,
    UUID categoryId,
    String categoryName,
    Integer brandId,
    String brandName,
    Integer modelId,
    String modelName,
    String realm,
    Integer yearModel,
    Integer yearBuild,
    Integer mileageKm,
    String fuel,
    String transmission,
    Map<String, Object> attributes,
    String status,
    String title,
    String description,
    BigDecimal price,
    String currency,
    Double latitude,
    Double longitude,
    String locationSource,
    UUID sellerUserId,
    String sellerName,
    String sellerWhatsapp,
    String sellerAvatarUrl,
    List<ProductImageView> images,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public record ProductImageView(
        UUID id, String url, String contentType, int sortOrder, boolean isCover
    ) {}
}
