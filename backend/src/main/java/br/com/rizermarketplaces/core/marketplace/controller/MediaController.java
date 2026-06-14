package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.dto.MediaPresignResponse;
import br.com.rizermarketplaces.core.marketplace.dto.MediaUploadResponse;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/media")
@Tag(name = "Media", description = "Upload e gerenciamento de arquivos (imagens públicas e documentos privados)")
public class MediaController {

    private final S3StorageService storageService;

    public MediaController(S3StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload de imagem (bucket público)",
        description = "Faz upload de uma imagem para o bucket público. A URL retornada é pública e pode ser acessada diretamente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upload realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido ou ausente"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<MediaUploadResponse> uploadImage(
        @Parameter(description = "Arquivo de imagem") @RequestParam("file") MultipartFile file,
        @Parameter(description = "Contexto do upload (ex: announce-gallery, store-logo)")
        @RequestParam(value = "context", defaultValue = "announce-gallery") String context
    ) throws Exception {
        MediaUploadResponse response = storageService.uploadPublicImage(file, context);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload de documento (bucket privado)",
        description = "Faz upload de um documento para o bucket privado. A URL retornada é temporária (presigned)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upload realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido ou ausente"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<MediaUploadResponse> uploadDocument(
        @Parameter(description = "Arquivo de documento") @RequestParam("file") MultipartFile file,
        @Parameter(description = "Contexto do upload (ex: contract, invoice)")
        @RequestParam(value = "context", defaultValue = "document") String context
    ) throws Exception {
        MediaUploadResponse response = storageService.uploadPrivateDocument(file, context);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/presign")
    @Operation(
        summary = "Gerar URL assinada para documento privado",
        description = "Gera uma nova URL presigned temporária para acessar um documento existente no bucket privado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL assinada gerada"),
        @ApiResponse(responseCode = "404", description = "Arquivo não encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<MediaPresignResponse> getPresignedUrl(
        @Parameter(description = "Chave do arquivo no bucket", required = true)
        @RequestParam String key
    ) {
        MediaPresignResponse response = storageService.getPresignedUrl(key);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(
        summary = "Remover arquivo",
        description = "Remove um arquivo de qualquer bucket (público ou privado)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Arquivo removido"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Arquivo não encontrado")
    })
    public ResponseEntity<Void> deleteFile(
        @Parameter(description = "Chave do arquivo no bucket", required = true)
        @RequestParam String key,
        @Parameter(description = "Nome do bucket (rizer-pic ou rizer-storage)")
        @RequestParam(value = "bucket", defaultValue = "rizer-storage") String bucket
    ) {
        storageService.deleteFile(key, bucket);
        return ResponseEntity.noContent().build();
    }
}
