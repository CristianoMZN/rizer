package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.AttachImageRequest;
import br.com.rizermarketplaces.core.marketplace.dto.ChangeStatusRequest;
import br.com.rizermarketplaces.core.marketplace.dto.CreateDraftRequest;
import br.com.rizermarketplaces.core.marketplace.dto.CreateProductRequest;
import br.com.rizermarketplaces.core.marketplace.dto.ProductView;
import br.com.rizermarketplaces.core.marketplace.dto.UpdateProductRequest;
import br.com.rizermarketplaces.core.marketplace.dto.UploadResponse;
import br.com.rizermarketplaces.core.marketplace.product.ProductImageService;
import br.com.rizermarketplaces.core.marketplace.product.ProductService;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/tenant/products")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Anúncios", description = "CRUD de produtos/anúncios do tenant")
public class TenantProductController {

    private final ProductService productService;
    private final ProductImageService imageService;

    public TenantProductController(ProductService productService, ProductImageService imageService) {
        this.productService = productService;
        this.imageService = imageService;
    }

    @GetMapping
    public List<ProductView> list() {
        return productService.listByTenant(requireTenant());
    }

    @GetMapping("/{id}")
    public ProductView get(@PathVariable UUID id) {
        return productService.get(requireTenant(), id);
    }

    @PostMapping
    public ResponseEntity<ProductView> create(@Valid @RequestBody CreateProductRequest req) {
        UUID tenantId = requireTenant();
        UUID actor = CurrentUser.require().getId();
        ProductView view = productService.create(tenantId, req, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PostMapping("/draft")
    public ResponseEntity<ProductView> createDraft(@Valid @RequestBody CreateDraftRequest req) {
        UUID tenantId = requireTenant();
        UUID actor = CurrentUser.require().getId();
        ProductView view = productService.createDraft(tenantId, req.physicalStoreId(), actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PatchMapping("/{id}")
    public ProductView update(@PathVariable UUID id, @RequestBody UpdateProductRequest req) {
        return productService.update(requireTenant(), id, req);
    }

    @PatchMapping("/{id}/status")
    public ProductView changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest req) {
        return productService.changeStatus(requireTenant(), id, req.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.softDelete(requireTenant(), id);
        return ResponseEntity.noContent().build();
    }

    // ─── Images ───────────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadImage(
        @PathVariable UUID id,
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "isCover", required = false) Boolean isCover
    ) throws IOException {
        return imageService.upload(requireTenant(), id, file, isCover);
    }

    @PostMapping("/{id}/images")
    public ProductView.ProductImageView attachImage(
        @PathVariable UUID id,
        @Valid @RequestBody AttachImageRequest req
    ) {
        return imageService.attach(requireTenant(), req);
    }

    @GetMapping("/{id}/images")
    public List<ProductView.ProductImageView> listImages(@PathVariable UUID id) {
        return imageService.list(requireTenant(), id);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        imageService.delete(requireTenant(), id, imageId);
        return ResponseEntity.noContent().build();
    }

    private UUID requireTenant() {
        UUID id = TenantContextHolder.getId();
        if (id == null) throw TenantExceptions.forbidden("Selecione um tenant antes de continuar");
        return id;
    }
}
