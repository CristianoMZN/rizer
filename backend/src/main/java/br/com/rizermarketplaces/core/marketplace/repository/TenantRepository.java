package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlugAndCountryCodeAndDeletedAtIsNull(String slug, String countryCode);

    Optional<Tenant> findBySlugAndDeletedAtIsNull(String slug);

    Optional<Tenant> findByCustomDomainIgnoreCaseAndDeletedAtIsNull(String customDomain);

    List<Tenant> findAllByIsPublicTrueAndIsPartnerPageEnabledTrueAndStatusAndDeletedAtIsNullOrderByTradeNameAsc(
        TenantStatus status
    );

    List<Tenant> findAllByStatusAndDeletedAtIsNull(TenantStatus status);

    boolean existsBySlugAndCountryCodeAndDeletedAtIsNull(String slug, String countryCode);
}
