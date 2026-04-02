package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySellerId(Long sellerId);

    List<Tenant> findAllByIsPublicTrueAndStatusOrderByNameAsc(String status);
}
