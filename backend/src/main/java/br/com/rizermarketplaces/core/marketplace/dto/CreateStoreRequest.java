package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateStoreRequest(
    @NotNull UUID tenantId,
    @NotNull @Size(max = 255) String name,
    @Size(max = 120) String slug,
    @Size(max = 32) String phone,
    @Size(max = 32) String whatsapp,
    @Size(max = 255) String email,
    @Size(max = 32) String adminPhone,
    @Size(max = 20) String cnpj,
    @Size(max = 255) String legalName,
    @Size(max = 512) String bannerUrl,
    Boolean isBranch,
    Boolean isMain,
    @Size(max = 16) String addressZipCode,
    @Size(max = 255) String addressStreet,
    @Size(max = 32) String addressNumber,
    @Size(max = 120) String addressComplement,
    @Size(max = 120) String addressNeighborhood,
    @Size(max = 120) String addressCity,
    @Size(max = 80) String addressState,
    Double latitude,
    Double longitude
) {}
