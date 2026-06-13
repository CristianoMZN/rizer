package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID tenantId);

    List<Product> findAllByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID tenantId, ProductStatus status
    );

    Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query(value = """
        SELECT p.* FROM products p
        WHERE p.deleted_at IS NULL
          AND p.status = 'ACTIVE'
          AND (:tenantId IS NULL OR p.tenant_id = CAST(:tenantId AS uuid))
          AND (:realm IS NULL OR p.realm = CAST(:realm AS varchar))
          AND (:categoryId IS NULL OR p.category_id = CAST(:categoryId AS uuid))
          AND (:brandId IS NULL OR p.brand_id = CAST(:brandId AS integer))
          AND (:minYear IS NULL OR p.year_model >= :minYear)
          AND (:maxYear IS NULL OR p.year_model <= :maxYear)
        ORDER BY p.created_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Product> search(
        String tenantId, String realm, String categoryId, Integer brandId,
        Short minYear, Short maxYear, int limit, int offset
    );

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, ProductStatus status);
}
