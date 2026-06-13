package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdateProductRequest(
    UUID physicalStoreId,
    UUID categoryId,
    Integer brandId,
    Integer modelId,
    @Size(max = 200) String title,
    @Size(max = 8000) String description,
    BigDecimal price,
    String currency,
    Integer yearModel,
    Integer yearBuild,
    Integer mileageKm,
    String fuel,
    String transmission,
    Map<String, Object> attributes,
    String status
) {}
