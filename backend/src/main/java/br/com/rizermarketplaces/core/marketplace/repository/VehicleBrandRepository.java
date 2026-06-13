package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Integer> {

    List<VehicleBrand> findAllByVehicleTypeOrderByNameAsc(String vehicleType);
}
