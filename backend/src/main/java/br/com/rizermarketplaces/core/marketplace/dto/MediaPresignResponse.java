package br.com.rizermarketplaces.core.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Resposta com URL assinada (presigned) para acesso a arquivo privado")
public record MediaPresignResponse(
    @Schema(description = "URL assinada temporária", example = "https://rizer-storage.br-se1.magaluobjects.com/docs/contract.pdf?X-Amz-Algorithm=...")
    String presignedUrl,

    @Schema(description = "Data/hora de expiração da assinatura (UTC)")
    Instant expiresAt
) {}
