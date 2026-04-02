package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.AttributeDefinition;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttributeMetadataRepository extends Repository<AttributeDefinition, Long> {

    @Query(value = """
        SELECT
            ad.id AS attributeDefinitionId,
            ad.code AS code,
            ad.data_type AS dataType,
            (ad.required OR cag.required) AS required,
            ad.validation_rules::text AS validationRules
        FROM category_attribute_groups cag
        JOIN attribute_definitions ad ON ad.group_id = cag.group_id
        WHERE cag.subsubcategory_id = :subsubcategoryId
          AND ad.active = TRUE
        ORDER BY ad.sort_order, ad.code
        """, nativeQuery = true)
    List<AttributeDefinitionRuleProjection> findRulesBySubsubcategoryId(@Param("subsubcategoryId") Long subsubcategoryId);
}
