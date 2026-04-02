package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Subsubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubsubcategoryRepository extends JpaRepository<Subsubcategory, Long> {

    Optional<Subsubcategory> findBySlug(String slug);
}
