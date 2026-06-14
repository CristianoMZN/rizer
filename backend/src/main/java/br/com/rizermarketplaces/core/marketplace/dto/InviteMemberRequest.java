package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record InviteMemberRequest(
    UUID tenantId,
    @NotBlank @Email String email,
    @NotBlank @Size(max = 255) String name,
    @NotNull TenantUserRole role,
    List<UUID> physicalStoreIds,
    @Size(max = 32) String whatsapp,
    @Size(max = 512) String avatarUrl,
    @Size(min = 6, max = 100) String password
) {}
