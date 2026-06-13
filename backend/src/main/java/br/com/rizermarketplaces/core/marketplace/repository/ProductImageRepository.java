package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findAllByProductIdOrderBySortOrderAscCreatedAtAsc(UUID productId);

    Optional<ProductImage> findByProductIdAndIsCoverTrue(UUID productId);

    long countByProductId(UUID productId);

    void deleteAllByProductId(UUID productId);
}
