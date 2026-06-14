package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    Optional<User> findByCpfAndDeletedAtIsNull(String cpf);

    List<User> findBySystemRoleAndDeletedAtIsNullOrderByNameAsc(br.com.rizermarketplaces.core.marketplace.model.SystemRole role);
}
