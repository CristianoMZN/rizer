package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.PhysicalStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhysicalStoreRepository extends JpaRepository<PhysicalStore, UUID> {

    List<PhysicalStore> findAllByTenantIdAndDeletedAtIsNullOrderByIsMainDescNameAsc(UUID tenantId);

    List<PhysicalStore> findAllByTenantIdAndIsActiveTrueAndDeletedAtIsNullOrderByIsMainDescNameAsc(UUID tenantId);

    Optional<PhysicalStore> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<PhysicalStore> findByTenantIdAndSlugAndDeletedAtIsNull(UUID tenantId, String slug);

    long countByTenantIdAndIsActiveTrueAndDeletedAtIsNull(UUID tenantId);

    boolean existsByTenantIdAndSlugAndDeletedAtIsNull(UUID tenantId, String slug);

    Optional<PhysicalStore> findByTenantIdAndIsMainTrueAndDeletedAtIsNull(UUID tenantId);
}
