package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.ProductLocalization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductLocalizationRepository extends JpaRepository<ProductLocalization, UUID> {

    Optional<ProductLocalization> findByProductIdAndCountryCode(UUID productId, String countryCode);

    List<ProductLocalization> findAllByProductId(UUID productId);
}
