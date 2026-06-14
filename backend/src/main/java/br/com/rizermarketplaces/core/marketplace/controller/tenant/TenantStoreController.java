package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.CreateStoreRequest;
import br.com.rizermarketplaces.core.marketplace.dto.StoreView;
import br.com.rizermarketplaces.core.marketplace.dto.UpdateStoreRequest;
import br.com.rizermarketplaces.core.marketplace.tenant.PhysicalStoreService;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tenant/stores")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Lojas", description = "CRUD de lojas físicas do tenant atual")
public class TenantStoreController {

    private final PhysicalStoreService service;

    public TenantStoreController(PhysicalStoreService service) {
        this.service = service;
    }

    @GetMapping
    public List<StoreView> list() {
        UUID tenantId = TenantContextHolder.requireId();
        return service.list(tenantId);
    }

    @PostMapping
    public ResponseEntity<StoreView> create(@Valid @RequestBody CreateStoreRequest req) {
        UUID tenantId = TenantContextHolder.requireId();
        UUID actorId = CurrentUser.require().getId();
        if (req.tenantId() != null && !req.tenantId().equals(tenantId)) {
            throw TenantExceptions.forbidden("Não é possível criar loja em outro tenant");
        }
        StoreView view = service.create(tenantId, req, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PatchMapping("/{id}")
    public StoreView update(@PathVariable UUID id, @RequestBody UpdateStoreRequest req) {
        return service.update(TenantContextHolder.requireId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.softDelete(TenantContextHolder.requireId(), id);
        return ResponseEntity.noContent().build();
    }

}
