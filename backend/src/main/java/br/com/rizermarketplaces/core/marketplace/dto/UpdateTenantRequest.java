package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
    @Size(max = 255) String tradeName,
    @Size(max = 255) String legalName,
    @Size(max = 20) String cnpj,
    @Size(max = 255) String description,
    @Size(max = 32) String phone,
    @Size(max = 32) String whatsapp,
    @Email @Size(max = 255) String email,
    @Size(max = 255) String website,
    @Size(max = 255) String logoUrl,
    @Size(max = 255) String bannerUrl,
    String status
) {}
