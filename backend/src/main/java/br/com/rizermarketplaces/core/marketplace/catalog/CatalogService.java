package br.com.rizermarketplaces.core.marketplace.catalog;

import br.com.rizermarketplaces.core.marketplace.dto.CategoryView;
import br.com.rizermarketplaces.core.marketplace.dto.VehicleBrandView;
import br.com.rizermarketplaces.core.marketplace.dto.VehicleModelView;
import br.com.rizermarketplaces.core.marketplace.model.VehicleBrand;
import br.com.rizermarketplaces.core.marketplace.model.VehicleModel;
import br.com.rizermarketplaces.core.marketplace.repository.CategoryRepository;
import br.com.rizermarketplaces.core.marketplace.repository.VehicleBrandRepository;
import br.com.rizermarketplaces.core.marketplace.repository.VehicleModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;

    public CatalogService(
        CategoryRepository categoryRepository,
        VehicleBrandRepository brandRepository,
        VehicleModelRepository modelRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.modelRepository = modelRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> listRoots(String countryCode) {
        return categoryRepository
            .findAllByCountryCodeAndLevelAndIsActiveTrueOrderBySortOrderAscNameAsc(countryCode, (short) 1)
            .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryView> listChildren(String countryCode, String realm) {
        var r = br.com.rizermarketplaces.core.marketplace.model.VehicleRealm.valueOf(realm.toUpperCase());
        return categoryRepository
            .findAllByCountryCodeAndRealmAndIsActiveTrueOrderBySortOrderAscNameAsc(countryCode, r)
            .stream().filter(c -> c.getLevel() == 2).map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleBrandView> listBrands(String vehicleType) {
        return brandRepository.findAllByVehicleTypeOrderByNameAsc(vehicleType)
            .stream().map(this::toBrandView).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleModelView> listModels(Integer brandId) {
        return modelRepository.findAllByBrandIdOrderByNameAsc(brandId)
            .stream().map(this::toModelView).toList();
    }

    private CategoryView toView(br.com.rizermarketplaces.core.marketplace.model.Category c) {
        return new CategoryView(
            c.getId(), c.getCountryCode(), c.getRealm().name(), c.getPath(),
            c.getName(), c.getSlug(), c.getParentId(), c.getLevel(),
            c.getSortOrder(), c.getIcon(), c.getImageUrl(), c.getDescription()
        );
    }

    private VehicleBrandView toBrandView(VehicleBrand b) {
        return new VehicleBrandView(b.getId(), b.getVehicleType(), b.getFipeId(), b.getName());
    }

    private VehicleModelView toModelView(VehicleModel m) {
        return new VehicleModelView(m.getId(), m.getBrandId(), m.getFipeId(), m.getName());
    }
}
