package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.GalleryImageView;
import br.com.rizermarketplaces.core.marketplace.tenant.PhysicalStoreGalleryService;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tenant/stores/{storeId}/gallery")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Galeria de Loja", description = "Imagens adicionais de uma loja específica")
public class StoreGalleryController {

    private final PhysicalStoreGalleryService service;

    public StoreGalleryController(PhysicalStoreGalleryService service) {
        this.service = service;
    }

    @GetMapping
    public List<GalleryImageView> list(@PathVariable UUID storeId) {
        return service.list(requireTenant(), storeId);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GalleryImageView upload(
        @PathVariable UUID storeId,
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "caption", required = false) String caption
    ) throws IOException {
        return service.upload(requireTenant(), storeId, file, caption);
    }

    @PatchMapping("/{id}/cover")
    public GalleryImageView setCover(@PathVariable UUID storeId, @PathVariable UUID id) {
        return service.setCover(requireTenant(), storeId, id);
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(@PathVariable UUID storeId, @RequestBody ReorderRequest req) {
        service.reorder(requireTenant(), storeId, req.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID storeId, @PathVariable UUID id) {
        service.delete(requireTenant(), storeId, id);
        return ResponseEntity.noContent().build();
    }

    public record ReorderRequest(List<UUID> ids) {}

    private UUID requireTenant() {
        UUID id = TenantContextHolder.getId();
        if (id == null) throw TenantExceptions.forbidden("Selecione um tenant antes de continuar");
        return id;
    }
}
