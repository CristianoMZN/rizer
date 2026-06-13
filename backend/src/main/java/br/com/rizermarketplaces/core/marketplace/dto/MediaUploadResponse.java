package br.com.rizermarketplaces.core.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta do upload de mídia")
public record MediaUploadResponse(
    @Schema(description = "URL pública ou presigned do arquivo", example = "https://rizer-pic.br-se1.magaluobjects.com/uploads/abc123.webp")
    String url,

    @Schema(description = "Chave do objeto no bucket", example = "uploads/abc123.webp")
    String key,

    @Schema(description = "Nome do bucket", example = "rizer-pic")
    String bucket,

    @Schema(description = "Content-Type do arquivo", example = "image/webp")
    String contentType,

    @Schema(description = "Tamanho em bytes", example = "81234")
    long sizeBytes
) {}
