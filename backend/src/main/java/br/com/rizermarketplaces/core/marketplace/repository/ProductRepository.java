package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID tenantId);

    List<Product> findAllByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID tenantId, ProductStatus status
    );

    Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    /**
     * Busca pública de produtos. Aplica filtros em colunas nativas e em
     * `products.attributes` (JSONB) através do operador `->>` do Postgres.
     * Cada filtro é opcional — se o parâmetro for NULL o predicate é descartado.
     *
     * Booleanos são aceitos como `true|false|null`. Quando `null`, não filtra.
     */
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
          AND (:fuel IS NULL OR p.attributes->>'fuel' = :fuel)
          AND (:transmission IS NULL OR p.attributes->>'transmission' = :transmission)
          AND (:transmissionDetail IS NULL OR p.attributes->>'transmission_detail' = :transmissionDetail)
          AND (:color IS NULL OR LOWER(p.attributes->>'color') = LOWER(:color))
          AND (:bodyType IS NULL OR p.attributes->>'body_type' = :bodyType)
          AND (:drivetrain IS NULL OR p.attributes->>'drivetrain' = :drivetrain)
          AND (:steering IS NULL OR p.attributes->>'steering' = :steering)
          AND (:condition IS NULL OR p.attributes->>'condition' = :condition)
          AND (:engine IS NULL OR p.attributes->>'engine' = :engine)
          AND (:cylinders IS NULL OR (p.attributes->>'cylinders')::int = :cylinders)
          AND (:armored IS NULL OR (p.attributes->>'armored')::boolean = :armored)
          AND (:abs IS NULL OR (p.attributes->>'abs_brakes')::boolean = :abs)
        ORDER BY p.created_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Product> search(
        @Param("tenantId") String tenantId,
        @Param("realm") String realm,
        @Param("categoryId") String categoryId,
        @Param("brandId") Integer brandId,
        @Param("minYear") Short minYear,
        @Param("maxYear") Short maxYear,
        @Param("fuel") String fuel,
        @Param("transmission") String transmission,
        @Param("transmissionDetail") String transmissionDetail,
        @Param("color") String color,
        @Param("bodyType") String bodyType,
        @Param("drivetrain") String drivetrain,
        @Param("steering") String steering,
        @Param("condition") String condition,
        @Param("engine") String engine,
        @Param("cylinders") Integer cylinders,
        @Param("armored") Boolean armored,
        @Param("abs") Boolean abs,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, ProductStatus status);
}
