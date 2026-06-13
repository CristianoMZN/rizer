package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByCountryCodeAndLevelAndIsActiveTrueOrderBySortOrderAscNameAsc(
        String countryCode, short level
    );

    List<Category> findAllByCountryCodeAndRealmAndIsActiveTrueOrderBySortOrderAscNameAsc(
        String countryCode, br.com.rizermarketplaces.core.marketplace.model.VehicleRealm realm
    );

    @Query("SELECT c FROM Category c WHERE c.countryCode = :cc AND c.path = CAST(:path AS string)")
    Optional<Category> findByCountryAndPath(String cc, String path);
}
