package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Consent;
import br.com.rizermarketplaces.core.marketplace.model.ConsentPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    @Query("SELECT c FROM Consent c WHERE c.userId = :userId AND c.purpose = :purpose ORDER BY c.createdAt DESC")
    List<Consent> findByUserAndPurpose(UUID userId, ConsentPurpose purpose);

    @Query("SELECT c FROM Consent c WHERE c.anonymousId = :anonymousId AND c.purpose = :purpose ORDER BY c.createdAt DESC")
    List<Consent> findByAnonymousAndPurpose(String anonymousId, ConsentPurpose purpose);
}
