package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.ProductLocalization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Repositório para a tabela de localizações de produto (projeções regionais).
public interface ProductLocalizationRepository extends JpaRepository<ProductLocalization, Long> {

    // Exemplo de consulta nativa (nativeQuery = true) usando funções PostGIS para busca geoespacial.
    // @Query: permite definir SQL customizado; @Param vincula parâmetros nomeados do SQL aos parâmetros do método.
    @Query(value = """
        SELECT
            p.uuid AS productUuid,
            CAST(p.realm AS varchar) AS realm,
            pl.country_code AS countryCode,
            pl.price AS price,
            pl.currency AS currency,
            ST_DistanceSphere(pl.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) / 1000.0 AS distanceKm
        FROM product_localizations pl
        JOIN products p ON p.id = pl.product_id
        WHERE pl.country_code = :countryCode
          AND (:realm IS NULL OR CAST(p.realm AS varchar) = :realm)
          AND ST_DWithin(
              pl.location::geography,
              ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
              :radiusMeters
          )
        ORDER BY distanceKm ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<NearbyProductProjection> findNearbyByCountryAndRealm(
        @Param("countryCode") String countryCode,
        @Param("realm") String realm,
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("radiusMeters") double radiusMeters,
        @Param("limit") int limit
    );
}
