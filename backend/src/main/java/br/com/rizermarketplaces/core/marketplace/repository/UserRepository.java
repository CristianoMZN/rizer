package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Repositório para a entidade User. Note que a chave primária aqui usa UUID.
public interface UserRepository extends JpaRepository<User, UUID> {

    // Deriva query por nome do método: busca um usuário pelo email
    Optional<User> findByEmail(String email);

    // Busca por combinação de provider (ex: "google") e providerId (id do usuário no provedor)
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}

