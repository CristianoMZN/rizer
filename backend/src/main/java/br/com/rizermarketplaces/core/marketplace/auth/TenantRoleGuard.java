package br.com.rizermarketplaces.core.marketplace.auth;

import br.com.rizermarketplaces.core.marketplace.model.TenantUser;
import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import br.com.rizermarketplaces.core.marketplace.repository.TenantUserRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Guard para ações sensíveis dentro de um tenant. Lê o papel do usuário
 * autenticado em {@code tenant_users} e aplica as regras:
 * <ul>
 *   <li><b>OWNER</b> — tudo: editar perfil, convidar membros, publicar, vender, arquivar.</li>
 *   <li><b>MANAGER</b> — publicar, marcar como vendido, arquivar; NÃO edita perfil do tenant (CNPJ, razão social, etc.).</li>
 *   <li><b>SELLER</b> — só rascunho (DRAFT/INACTIVE). NÃO publica, NÃO marca como vendido.</li>
 * </ul>
 */
@Component
public class TenantRoleGuard {

    private final TenantUserRepository tenantUserRepository;

    public TenantRoleGuard(TenantUserRepository tenantUserRepository) {
        this.tenantUserRepository = tenantUserRepository;
    }

    public TenantUserRole currentRole(UUID tenantId) {
        AuthenticatedUser u = CurrentUser.require();
        if (tenantId == null) {
            throw TenantExceptions.forbidden("Selecione um tenant antes de continuar");
        }
        return tenantUserRepository
            .findByTenantIdAndUserIdAndIsActiveTrue(tenantId, u.getId())
            .map(TenantUser::getRole)
            .orElseThrow(() -> TenantExceptions.forbidden("Você não é membro deste tenant"));
    }

    public boolean isAtLeast(UUID tenantId, TenantUserRole required) {
        TenantUserRole role = currentRole(tenantId);
        return rank(role) >= rank(required);
    }

    public void requireAtLeast(UUID tenantId, TenantUserRole required) {
        if (!isAtLeast(tenantId, required)) {
            throw TenantExceptions.forbidden(
                "Apenas " + label(required) + "+ pode realizar esta ação"
            );
        }
    }

    public void assertCanPublish(UUID tenantId) {
        requireAtLeast(tenantId, TenantUserRole.MANAGER);
    }

    public void assertCanMarkSold(UUID tenantId) {
        requireAtLeast(tenantId, TenantUserRole.MANAGER);
    }

    public void assertCanManageTenant(UUID tenantId) {
        requireAtLeast(tenantId, TenantUserRole.OWNER);
    }

    public void assertCanInviteMembers(UUID tenantId) {
        requireAtLeast(tenantId, TenantUserRole.OWNER);
    }

    private int rank(TenantUserRole role) {
        return switch (role) {
            case SELLER -> 1;
            case MANAGER -> 2;
            case OWNER -> 3;
        };
    }

    private String label(TenantUserRole role) {
        return switch (role) {
            case SELLER -> "vendedor";
            case MANAGER -> "gerente";
            case OWNER -> "proprietário";
        };
    }
}
