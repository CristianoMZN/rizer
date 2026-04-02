package br.com.rizermarketplaces.core.marketplace.repository;

import java.math.BigDecimal;
import java.util.UUID;

// Interface de projeção usada pelo Spring Data para mapear o resultado da query nativa
// para um objeto sem a necessidade de criar uma classe DTO concreta.
public interface NearbyProductProjection {

    UUID getProductUuid();

    String getRealm();

    String getCountryCode();

    BigDecimal getPrice();

    String getCurrency();

    Double getDistanceKm();
}
