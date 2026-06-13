package br.com.rizermarketplaces.core.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicProductView(
    UUID id,
    String title,
    String description,
    BigDecimal price,
    String currency,
    String realm,
    Integer yearModel,
    Integer yearBuild,
    Integer mileageKm,
    String fuel,
    String transmission,
    String brandName,
    String modelName,
    String categoryName,
    UUID physicalStoreId,
    String physicalStoreName,
    String physicalStoreCity,
    String physicalStoreState,
    Map<String, Object> attributes,
    List<PublicProductImage> images,
    String createdAt
) {
    public record PublicProductImage(String id, String url, boolean isCover) {}
}
