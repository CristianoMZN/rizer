package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdateProductRequest(
    UUID physicalStoreId,
    UUID categoryId,
    Integer brandId,
    Integer modelId,
    String title,
    String description,
    BigDecimal price,
    String currency,
    Integer yearModel,
    Integer yearBuild,
    Integer mileageKm,
    String fuel,
    String transmission,
    Map<String, Object> attributes,
    ProductStatus status,
    UUID sellerUserId,
    Double latitude,
    Double longitude
) {}
