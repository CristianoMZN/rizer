package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.PhysicalStoreGalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhysicalStoreGalleryImageRepository extends JpaRepository<PhysicalStoreGalleryImage, UUID> {

    List<PhysicalStoreGalleryImage> findAllByPhysicalStoreIdOrderBySortOrderAscCreatedAtAsc(UUID physicalStoreId);

    Optional<PhysicalStoreGalleryImage> findByIdAndPhysicalStoreId(UUID id, UUID physicalStoreId);

    Optional<PhysicalStoreGalleryImage> findByPhysicalStoreIdAndIsCoverTrue(UUID physicalStoreId);

    long countByPhysicalStoreId(UUID physicalStoreId);

    void deleteAllByPhysicalStoreId(UUID physicalStoreId);
}
