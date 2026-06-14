package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
    @NotNull ProductStatus status
) {}
