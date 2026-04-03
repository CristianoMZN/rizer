package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.AttributeSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AttributeSchemaRepository extends JpaRepository<AttributeSchema, Long> {

    @Query(value = """
        SELECT *
        FROM attribute_schemas s
        WHERE s.entity_type = :entityType
          AND s.category_path = CAST(:categoryPath AS ltree)
          AND s.country_code IN (:countryCode, '*')
          AND s.is_active = TRUE
        ORDER BY CASE WHEN s.country_code = :countryCode THEN 0 ELSE 1 END,
                 s.version DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<AttributeSchema> findActiveByContext(
        @Param("entityType") String entityType,
        @Param("countryCode") String countryCode,
        @Param("categoryPath") String categoryPath
    );
}