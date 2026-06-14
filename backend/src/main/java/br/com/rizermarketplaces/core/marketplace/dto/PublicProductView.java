package br.com.rizermarketplaces.core.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    String physicalStoreBannerUrl,
    Map<String, Object> attributes,
    List<PublicProductImage> images,
    String createdAt,
    String tenantSlug,
    String tenantTradeName,
    String tenantLogoUrl,
    String tenantWhatsapp,
    String tenantPhone,
    String sellerUserId,
    String sellerName,
    String sellerWhatsapp,
    String sellerAvatarUrl,
    Double latitude,
    Double longitude
) {
    public record PublicProductImage(String id, String url, boolean isCover) {}
}
