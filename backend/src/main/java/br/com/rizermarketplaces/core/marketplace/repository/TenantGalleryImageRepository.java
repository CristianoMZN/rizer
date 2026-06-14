package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.TenantGalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantGalleryImageRepository extends JpaRepository<TenantGalleryImage, UUID> {

    List<TenantGalleryImage> findAllByTenantIdOrderBySortOrderAscCreatedAtAsc(UUID tenantId);

    Optional<TenantGalleryImage> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<TenantGalleryImage> findByTenantIdAndIsCoverTrue(UUID tenantId);

    long countByTenantId(UUID tenantId);

    void deleteAllByTenantId(UUID tenantId);
}
