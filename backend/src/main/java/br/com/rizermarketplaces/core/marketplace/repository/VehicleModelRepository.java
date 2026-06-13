package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Integer> {

    List<VehicleModel> findAllByBrandIdOrderByNameAsc(Integer brandId);
}
