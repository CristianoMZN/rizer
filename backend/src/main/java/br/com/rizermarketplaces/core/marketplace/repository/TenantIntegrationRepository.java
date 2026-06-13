package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.IntegrationProvider;
import br.com.rizermarketplaces.core.marketplace.model.IntegrationStatus;
import br.com.rizermarketplaces.core.marketplace.model.TenantIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantIntegrationRepository extends JpaRepository<TenantIntegration, UUID> {

    Optional<TenantIntegration> findByTenantIdAndProvider(UUID tenantId, IntegrationProvider provider);

    List<TenantIntegration> findAllByTenantIdOrderByProviderAsc(UUID tenantId);

    List<TenantIntegration> findAllByProviderAndStatus(IntegrationProvider provider, IntegrationStatus status);
}
