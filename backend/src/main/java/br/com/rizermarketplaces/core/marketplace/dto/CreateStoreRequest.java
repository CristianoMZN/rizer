package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateStoreRequest(
    @NotNull UUID tenantId,
    @NotNull @Size(max = 255) String name,
    @Size(max = 120) String slug,
    @Size(max = 32) String phone,
    @Size(max = 32) String whatsapp,
    @Size(max = 255) String email,
    Boolean isMain,
    Double latitude,
    Double longitude
) {}
