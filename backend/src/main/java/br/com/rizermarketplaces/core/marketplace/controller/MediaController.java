package br.com.rizermarketplaces.core.marketplace.controller;

/*
 * Controlador para upload e processamento de mídia (imagens).
 * Explica como Spring trata multipart/form-data, validações e integrações com segurança.
 */

import br.com.rizermarketplaces.core.marketplace.dto.media.MediaUploadResponse;
import br.com.rizermarketplaces.core.marketplace.service.MediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
@Validated
@Tag(name = "Media", description = "Upload e processamento de imagens com armazenamento no S3")
public class MediaController {

    private final MediaStorageService mediaStorageService;

    // Injeção do serviço responsável por armazenar/processar a mídia.
    public MediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    // @PostMapping com consumes = MULTIPART_FORM_DATA_VALUE: indica que o endpoint aceita uploads multipart.
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload de imagem autenticado",
        description = "Recebe a imagem e metadados, aplica regras por contexto, envia para bucket privado no S3 e devolve URL assinada.",
        // @SecurityRequirement: indica que este endpoint exige autenticação conforme esquema definido no OpenAPI.
        security = @SecurityRequirement(name = "oauth2")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Upload concluido",
            content = @Content(schema = @Schema(implementation = MediaUploadResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Payload invalido"),
        @ApiResponse(responseCode = "401", description = "Usuario nao autenticado")
    })
    public ResponseEntity<MediaUploadResponse> upload(
        // @RequestParam("file"): vincula o campo multipart 'file' ao parâmetro MultipartFile
        @RequestParam("file") MultipartFile file,
        @RequestParam("type") String type,
        @RequestParam("context") String context,
        // @Min(1): valida que width/height, se fornecidos, sejam >= 1
        @RequestParam(value = "width", required = false) @Min(1) Integer width,
        @RequestParam(value = "height", required = false) @Min(1) Integer height
    ) {
        MediaUploadResponse response = mediaStorageService.uploadPicture(file, type, context, width, height);
        return ResponseEntity.ok(response);
    }
}
