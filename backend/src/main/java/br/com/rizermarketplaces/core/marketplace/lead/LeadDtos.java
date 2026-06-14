package br.com.rizermarketplaces.core.marketplace.lead;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeadDtos() {

    public record CreateLeadRequest(
        UUID productId,
        UUID storeId,
        @NotBlank @Size(max = 120) String buyerName,
        @Size(max = 255) String buyerEmail,
        @NotBlank @Size(max = 20) String buyerPhone,
        String message
    ) {}

    public record LeadView(
        UUID id,
        UUID tenantId,
        UUID productId,
        UUID physicalStoreId,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        String message,
        String status,
        UUID sellerUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}

    public record UpdateStatusRequest(
        String status
    ) {}
}
