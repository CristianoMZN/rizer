package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    List<Lead> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Lead> findByPhysicalStoreIdOrderByCreatedAtDesc(UUID physicalStoreId);
}
