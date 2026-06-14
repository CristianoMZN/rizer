package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.auth.TenantRoleGuard;
import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.InviteMemberRequest;
import br.com.rizermarketplaces.core.marketplace.dto.MemberView;
import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantMemberService;
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
@RequestMapping("/tenant/members")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Membros", description = "Convite e gestão de membros do tenant atual")
public class TenantMemberController {

    private final TenantMemberService service;
    private final TenantRoleGuard roleGuard;

    public TenantMemberController(TenantMemberService service, TenantRoleGuard roleGuard) {
        this.service = service;
        this.roleGuard = roleGuard;
    }

    @GetMapping
    public List<MemberView> list() {
        return service.list(TenantContextHolder.requireId());
    }

    @PostMapping
    public ResponseEntity<MemberView> invite(@Valid @RequestBody InviteMemberRequest req) {
        UUID tenantId = TenantContextHolder.requireId();
        roleGuard.assertCanInviteMembers(tenantId);
        if (req.tenantId() == null) {
            req = new InviteMemberRequest(
                tenantId, req.email(), req.name(), req.role(), req.physicalStoreIds(),
                req.whatsapp(), req.avatarUrl(), req.password()
            );
        } else if (!req.tenantId().equals(tenantId)) {
            throw TenantExceptions.forbidden("Não é possível convidar membro para outro tenant");
        }
        UUID actorId = CurrentUser.require().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(req, actorId));
    }

    @PatchMapping("/{id}")
    public MemberView update(
        @PathVariable UUID id,
        @RequestBody UpdateMemberRequest body
    ) {
        UUID tenantId = TenantContextHolder.requireId();
        roleGuard.requireAtLeast(tenantId, TenantUserRole.MANAGER);
        TenantUserRole role = body.role() != null ? body.role() : null;
        return service.updateRole(tenantId, id, role == null ? TenantUserRole.SELLER : role, body.physicalStoreIds());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.requireId();
        roleGuard.assertCanInviteMembers(tenantId);
        service.remove(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    public record UpdateMemberRequest(TenantUserRole role, List<UUID> physicalStoreIds) {}
}
