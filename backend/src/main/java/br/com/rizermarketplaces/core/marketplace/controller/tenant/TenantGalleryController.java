package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.GalleryImageView;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantGalleryService;
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
@RequestMapping("/tenant/settings/gallery")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Galeria", description = "Imagens adicionais do tenant (fachada, interior, pátio, equipe)")
public class TenantGalleryController {

    private final TenantGalleryService service;

    public TenantGalleryController(TenantGalleryService service) {
        this.service = service;
    }

    @GetMapping
    public List<GalleryImageView> list() {
        return service.list(requireTenant());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GalleryImageView upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "caption", required = false) String caption
    ) throws IOException {
        return service.upload(requireTenant(), file, caption);
    }

    @PatchMapping("/{id}/cover")
    public GalleryImageView setCover(@PathVariable UUID id) {
        return service.setCover(requireTenant(), id);
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody ReorderRequest req) {
        service.reorder(requireTenant(), req.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(requireTenant(), id);
        return ResponseEntity.noContent().build();
    }

    public record ReorderRequest(List<UUID> ids) {}

    private UUID requireTenant() {
        UUID id = TenantContextHolder.getId();
        if (id == null) throw TenantExceptions.forbidden("Selecione um tenant antes de continuar");
        return id;
    }
}
