package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.AttributeSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttributeSchemaRepository extends JpaRepository<AttributeSchema, UUID> {

    @Query(value = """
        SELECT * FROM attribute_schemas
        WHERE country_code = :countryCode
          AND entity_type = :entityType
          AND is_active = TRUE
          AND (realm IS NULL OR realm = :realm)
          AND (CAST(:categoryPath AS ltree) <@ category_path)
        ORDER BY nlevel(category_path) DESC, version DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<AttributeSchema> findActiveFor(
        String countryCode, String entityType, String realm, String categoryPath
    );

    List<AttributeSchema> findAllByIsActiveTrue();
}
