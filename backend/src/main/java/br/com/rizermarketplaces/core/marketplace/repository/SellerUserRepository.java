package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.SellerUser;
import br.com.rizermarketplaces.core.marketplace.model.SellerUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface SellerUserRepository extends JpaRepository<SellerUser, Long> {

    boolean existsBySellerIdAndUserIdAndActiveTrueAndRoleIn(Long sellerId, UUID userId, Collection<SellerUserRole> roles);
}
