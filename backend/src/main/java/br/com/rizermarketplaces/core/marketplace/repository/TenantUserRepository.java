package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.TenantUser;
import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {

    Optional<TenantUser> findByTenantIdAndUserIdAndIsActiveTrue(UUID tenantId, UUID userId);

    List<TenantUser> findAllByUserIdAndIsActiveTrue(UUID userId);

    List<TenantUser> findAllByTenantIdAndIsActiveTrue(UUID tenantId);

    boolean existsByTenantIdAndUserIdAndIsActiveTrueAndRoleIn(
        UUID tenantId, UUID userId, List<TenantUserRole> roles
    );

    long countByTenantIdAndIsActiveTrue(UUID tenantId);
}
