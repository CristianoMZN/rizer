package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
    @NotBlank @Size(max = 80) String slug,
    @NotBlank @Size(max = 255) String tradeName,
    @Size(max = 255) String legalName,
    @Size(max = 20) String cnpj,
    @NotBlank @Size(min = 2, max = 2) String countryCode,
    @Size(max = 255) String description,
    @Size(max = 32) String phone,
    @Size(max = 32) String whatsapp,
    @Email @Size(max = 255) String email,
    @Size(max = 255) String website,
    // 1º owner
    @NotBlank @Size(max = 255) String ownerName,
    @NotBlank @Email @Size(max = 255) String ownerEmail,
    @Pattern(regexp = "^\\+?[\\d\\s().-]{6,32}$", message = "Telefone inválido") String ownerPhone,
    @Size(min = 8, max = 128) String ownerPassword,
    boolean startWithTrial
) {}
