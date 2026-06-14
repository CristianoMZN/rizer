package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.CepLookupView;
import br.com.rizermarketplaces.core.marketplace.dto.CreateStoreRequest;
import br.com.rizermarketplaces.core.marketplace.dto.StoreView;
import br.com.rizermarketplaces.core.marketplace.dto.UpdateStoreRequest;
import br.com.rizermarketplaces.core.marketplace.model.PhysicalStore;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.tools.SlugGenerator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PhysicalStoreService {

    private final PhysicalStoreRepository repository;
    private final TenantRepository tenantRepository;
    private final StoreLimitGuard storeLimitGuard;
    private final CepLookupService cepLookupService;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public PhysicalStoreService(
        PhysicalStoreRepository repository,
        TenantRepository tenantRepository,
        StoreLimitGuard storeLimitGuard,
        CepLookupService cepLookupService
    ) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.storeLimitGuard = storeLimitGuard;
        this.cepLookupService = cepLookupService;
    }

    @Transactional(readOnly = true)
    public List<StoreView> list(UUID tenantId) {
        ensureTenant(tenantId);
        return repository.findAllByTenantIdAndDeletedAtIsNullOrderByIsMainDescNameAsc(tenantId)
            .stream().map(TenantMapper::toStoreView).toList();
    }

    @Transactional
    public StoreView create(UUID tenantId, CreateStoreRequest req, UUID actorId) {
        ensureTenant(tenantId);
        storeLimitGuard.assertCanCreate(tenantId);

        String slug = req.slug() != null && !req.slug().isBlank()
            ? SlugGenerator.from(req.slug())
            : SlugGenerator.from(req.name());
        if (slug.isBlank()) throw TenantExceptions.badRequest("Slug/nome inválido");
        if (repository.existsByTenantIdAndSlugAndDeletedAtIsNull(tenantId, slug)) {
            throw TenantExceptions.conflict("Já existe uma loja com este slug neste tenant");
        }

        boolean wantsMain = Boolean.TRUE.equals(req.isMain());
        if (wantsMain) clearMainFlag(tenantId);

        Double lat = req.latitude();
        Double lng = req.longitude();
        if ((lat == null || lng == null) && req.addressZipCode() != null && !req.addressZipCode().isBlank()) {
            CepLookupView geo = cepLookupService.lookup(req.addressZipCode());
            if (geo != null) {
                if (lat == null) lat = geo.latitude();
                if (lng == null) lng = geo.longitude();
            }
        }

        PhysicalStore s = new PhysicalStore();
        s.setTenantId(tenantId);
        s.setName(req.name().trim());
        s.setSlug(slug);
        s.setPhone(req.phone());
        s.setWhatsapp(req.whatsapp());
        s.setEmail(req.email());
        s.setAdminPhone(req.adminPhone());
        s.setCnpj(req.cnpj());
        s.setLegalName(req.legalName());
        s.setBannerUrl(req.bannerUrl());
        s.setBranch(Boolean.TRUE.equals(req.isBranch()));
        s.setMain(wantsMain);
        s.setActive(true);
        s.setLocation(toPoint(lat, lng));
        s.setAddressZipCode(req.addressZipCode());
        s.setAddressStreet(req.addressStreet());
        s.setAddressNumber(req.addressNumber());
        s.setAddressComplement(req.addressComplement());
        s.setAddressNeighborhood(req.addressNeighborhood());
        s.setAddressCity(req.addressCity());
        s.setAddressState(req.addressState());
        s.setCreatedByUserId(actorId);

        PhysicalStore saved = repository.save(s);
        return TenantMapper.toStoreView(saved);
    }

    @Transactional
    public StoreView update(UUID tenantId, UUID storeId, UpdateStoreRequest req) {
        PhysicalStore s = repository.findByIdAndTenantIdAndDeletedAtIsNull(storeId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Loja"));
        if (req.name() != null) s.setName(req.name().trim());
        if (req.phone() != null) s.setPhone(req.phone());
        if (req.whatsapp() != null) s.setWhatsapp(req.whatsapp());
        if (req.email() != null) s.setEmail(req.email());
        if (req.adminPhone() != null) s.setAdminPhone(req.adminPhone());
        if (req.cnpj() != null) s.setCnpj(req.cnpj());
        if (req.legalName() != null) s.setLegalName(req.legalName());
        if (req.bannerUrl() != null) s.setBannerUrl(req.bannerUrl());
        if (req.isBranch() != null) s.setBranch(req.isBranch());
        if (Boolean.TRUE.equals(req.isMain())) {
            clearMainFlag(tenantId);
            s.setMain(true);
        } else if (Boolean.FALSE.equals(req.isMain())) {
            s.setMain(false);
        }
        if (Boolean.TRUE.equals(req.isActive())) s.setActive(true);
        else if (Boolean.FALSE.equals(req.isActive())) s.setActive(false);
        if (req.addressZipCode() != null) s.setAddressZipCode(req.addressZipCode());
        if (req.addressStreet() != null) s.setAddressStreet(req.addressStreet());
        if (req.addressNumber() != null) s.setAddressNumber(req.addressNumber());
        if (req.addressComplement() != null) s.setAddressComplement(req.addressComplement());
        if (req.addressNeighborhood() != null) s.setAddressNeighborhood(req.addressNeighborhood());
        if (req.addressCity() != null) s.setAddressCity(req.addressCity());
        if (req.addressState() != null) s.setAddressState(req.addressState());

        Double lat = req.latitude();
        Double lng = req.longitude();
        if ((lat == null || lng == null) && req.addressZipCode() != null) {
            CepLookupView geo = cepLookupService.lookup(req.addressZipCode());
            if (geo != null) {
                if (lat == null) lat = geo.latitude();
                if (lng == null) lng = geo.longitude();
            }
        }
        if (lat != null && lng != null) {
            s.setLocation(toPoint(lat, lng));
        }
        return TenantMapper.toStoreView(repository.save(s));
    }

    @Transactional
    public void softDelete(UUID tenantId, UUID storeId) {
        PhysicalStore s = repository.findByIdAndTenantIdAndDeletedAtIsNull(storeId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Loja"));
        s.setDeletedAt(OffsetDateTime.now());
        s.setActive(false);
        if (s.isMain()) s.setMain(false);
        repository.save(s);
    }

    private void ensureTenant(UUID tenantId) {
        Tenant t = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        if (t.getDeletedAt() != null) throw TenantExceptions.notFound("Tenant");
    }

    private void clearMainFlag(UUID tenantId) {
        repository.findByTenantIdAndIsMainTrueAndDeletedAtIsNull(tenantId).ifPresent(existing -> {
            if (!existing.isMain()) return;
            existing.setMain(false);
            repository.save(existing);
        });
    }

    private org.locationtech.jts.geom.Point toPoint(Double lat, Double lng) {
        if (lat == null || lng == null) return null;
        return geometryFactory.createPoint(new Coordinate(lng, lat));
    }
}
