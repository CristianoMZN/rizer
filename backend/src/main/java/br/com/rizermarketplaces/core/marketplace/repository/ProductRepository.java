package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Repository Spring Data JPA para a entidade Product.
// Extender JpaRepository fornece métodos CRUD prontos (findAll, findById, save, delete, etc.).
// Generics: <Product, Long> -> entidade Product com chave primária do tipo Long.
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Query derivation: Spring Data implementa automaticamente o método procurando por 'uuid' na entidade.
    Optional<Product> findByUuid(UUID uuid);
}
