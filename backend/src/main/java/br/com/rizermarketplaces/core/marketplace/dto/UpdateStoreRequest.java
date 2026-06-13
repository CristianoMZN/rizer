package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateStoreRequest(
    @Size(max = 255) String name,
    @Size(max = 32) String phone,
    @Size(max = 32) String whatsapp,
    @Size(max = 255) String email,
    Boolean isMain,
    Boolean isActive,
    Double latitude,
    Double longitude
) {}
