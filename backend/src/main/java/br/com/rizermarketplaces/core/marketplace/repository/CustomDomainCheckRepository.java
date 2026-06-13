package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.CustomDomainCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomDomainCheckRepository extends JpaRepository<CustomDomainCheck, UUID> {
    List<CustomDomainCheck> findTop10ByTenantIdOrderByCheckedAtDesc(UUID tenantId);
}
