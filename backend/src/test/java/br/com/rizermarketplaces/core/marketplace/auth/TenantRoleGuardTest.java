package br.com.rizermarketplaces.core.marketplace.auth;

import br.com.rizermarketplaces.core.marketplace.model.SystemRole;
import br.com.rizermarketplaces.core.marketplace.model.TenantUser;
import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import br.com.rizermarketplaces.core.marketplace.repository.TenantUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantRoleGuardTest {

    private TenantUserRepository repo;
    private TenantRoleGuard guard;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repo = mock(TenantUserRepository.class);
        guard = new TenantRoleGuard(repo);
        AuthenticatedUser u = new AuthenticatedUser(userId, "u@e.com", "User", SystemRole.agency_employee, true);
        u.setCurrentTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                u, null, u.getAuthorities()
            )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentRole_seller() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.SELLER)));
        assertEquals(TenantUserRole.SELLER, guard.currentRole(tenantId));
    }

    @Test
    void currentRole_semVinculo_lanca403() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> guard.currentRole(tenantId));
    }

    @Test
    void isAtLeast_seller_naoEhManager() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.SELLER)));
        assertFalse(guard.isAtLeast(tenantId, TenantUserRole.MANAGER));
    }

    @Test
    void isAtLeast_manager_naoEhOwner() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.MANAGER)));
        assertTrue(guard.isAtLeast(tenantId, TenantUserRole.MANAGER));
        assertFalse(guard.isAtLeast(tenantId, TenantUserRole.OWNER));
    }

    @Test
    void isAtLeast_owner_ehTudo() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.OWNER)));
        assertTrue(guard.isAtLeast(tenantId, TenantUserRole.OWNER));
        assertTrue(guard.isAtLeast(tenantId, TenantUserRole.MANAGER));
        assertTrue(guard.isAtLeast(tenantId, TenantUserRole.SELLER));
    }

    @Test
    void assertCanPublish_seller_bloqueado() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.SELLER)));
        assertThrows(ResponseStatusException.class, () -> guard.assertCanPublish(tenantId));
    }

    @Test
    void assertCanPublish_manager_ok() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.MANAGER)));
        assertDoesNotThrow(() -> guard.assertCanPublish(tenantId));
    }

    @Test
    void assertCanMarkSold_seller_bloqueado() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.SELLER)));
        assertThrows(ResponseStatusException.class, () -> guard.assertCanMarkSold(tenantId));
    }

    @Test
    void assertCanManageTenant_apenasOwner() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.OWNER)));
        assertDoesNotThrow(() -> guard.assertCanManageTenant(tenantId));

        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.MANAGER)));
        assertThrows(ResponseStatusException.class, () -> guard.assertCanManageTenant(tenantId));
    }

    @Test
    void assertCanInviteMembers_apenasOwner() {
        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.OWNER)));
        assertDoesNotThrow(() -> guard.assertCanInviteMembers(tenantId));

        when(repo.findByTenantIdAndUserIdAndIsActiveTrue(tenantId, userId))
            .thenReturn(Optional.of(member(TenantUserRole.MANAGER)));
        assertThrows(ResponseStatusException.class, () -> guard.assertCanInviteMembers(tenantId));
    }

    private TenantUser member(TenantUserRole role) {
        TenantUser tu = new TenantUser();
        tu.setTenantId(tenantId);
        tu.setUserId(userId);
        tu.setRole(role);
        tu.setActive(true);
        return tu;
    }
}
