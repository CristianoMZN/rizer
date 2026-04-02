package br.com.rizermarketplaces.core.marketplace.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO imutável representando a resposta do upload de mídia.
@Schema(name = "MediaUploadResponse", description = "Dados do arquivo processado e armazenado no S3")
public record MediaUploadResponse(
    @Schema(description = "Tipo de midia informado no upload", example = "picture")
    String type,
    @Schema(description = "Contexto de processamento aplicado", example = "announce-gallery")
    String context,
    @Schema(description = "Chave do objeto no bucket", example = "uploads/announce-gallery/9f8e7d6c4b3a.webp")
    String objectKey,
    @Schema(description = "URL base do objeto no S3 (bucket privado)", example = "https://bucket.s3.us-east-1.amazonaws.com/uploads/announce-gallery/9f8e7d6c4b3a.webp")
    String objectUrl,
    @Schema(description = "URL assinada temporaria para exibicao da imagem", example = "https://bucket.s3.us-east-1.amazonaws.com/uploads/announce-gallery/9f8e7d6c4b3a.webp?X-Amz-Algorithm=AWS4-HMAC-SHA256&...")
    String accessUrl,
    @Schema(description = "Token de autorizacao embutido na URL assinada", example = "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...")
    String authorizationToken,
    @Schema(description = "Largura final da imagem em px", example = "400")
    int width,
    @Schema(description = "Altura final da imagem em px", example = "400")
    int height,
    @Schema(description = "Formato final da imagem", example = "webp")
    String format,
    @Schema(description = "Tamanho final em bytes", example = "81234")
    long sizeBytes
) {
}
