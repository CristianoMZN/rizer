package br.com.rizermarketplaces.core.marketplace.controller.admin;

import br.com.rizermarketplaces.core.marketplace.admin.AdminTenantService;
import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.dto.CreateTenantRequest;
import br.com.rizermarketplaces.core.marketplace.dto.TenantView;
import br.com.rizermarketplaces.core.marketplace.dto.UpdateTenantRequest;
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
@RequestMapping("/admin/tenants")
@PreAuthorize("hasAnyRole('sys_admin','sys_manager')")
@Tag(name = "Admin · Tenants", description = "CRUD administrativo de tenants")
public class AdminTenantController {

    private final AdminTenantService adminTenantService;

    public AdminTenantController(AdminTenantService adminTenantService) {
        this.adminTenantService = adminTenantService;
    }

    @GetMapping
    public List<TenantView> list() {
        return adminTenantService.listAll();
    }

    @GetMapping("/{id}")
    public TenantView get(@PathVariable UUID id) {
        return adminTenantService.getById(id);
    }

    @PostMapping
    public ResponseEntity<TenantView> create(@Valid @RequestBody CreateTenantRequest req) {
        var principal = CurrentUser.require();
        TenantView view = adminTenantService.createTenant(req, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PatchMapping("/{id}")
    public TenantView update(@PathVariable UUID id, @RequestBody UpdateTenantRequest req) {
        return adminTenantService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminTenantService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
