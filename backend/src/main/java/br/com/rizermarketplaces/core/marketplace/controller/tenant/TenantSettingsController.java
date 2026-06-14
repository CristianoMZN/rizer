package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.auth.TenantRoleGuard;
import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.model.CustomDomainCheck;
import br.com.rizermarketplaces.core.marketplace.model.CustomDomainStatus;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.CepLookupService;
import br.com.rizermarketplaces.core.marketplace.tenant.CustomDomainService;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import br.com.rizermarketplaces.core.marketplace.dto.CepLookupView;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tenant/settings")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Configurações", description = "Perfil do tenant, tema e domínio customizado")
public class TenantSettingsController {

    private static final Logger log = LoggerFactory.getLogger(TenantSettingsController.class);
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);

    private final TenantRepository tenantRepository;
    private final CustomDomainService customDomainService;
    private final TenantRoleGuard roleGuard;
    private final CepLookupService cepLookupService;

    public TenantSettingsController(
        TenantRepository tenantRepository,
        CustomDomainService customDomainService,
        TenantRoleGuard roleGuard,
        CepLookupService cepLookupService
    ) {
        this.tenantRepository = tenantRepository;
        this.customDomainService = customDomainService;
        this.roleGuard = roleGuard;
        this.cepLookupService = cepLookupService;
    }

    @GetMapping
    public br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView get() {
        Tenant t = load();
        return br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView.of(t);
    }

    @PostMapping("/profile")
    public br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView updateProfile(@RequestBody UpdateProfileRequest req) {
        UUID tenantId = TenantContextHolder.requireId();
        // Apenas OWNER pode editar o perfil do tenant. MANAGER/SELLER podem
        // visualizar mas não alterar dados cadastrais da empresa.
        roleGuard.requireAtLeast(tenantId, TenantUserRole.OWNER);
        Tenant t = load();

        // Campos read-only (CNPJ, razão social, trade name, slug, sócio proprietário):
        // aceitos no payload mas IGNORADOS no backend. Edição requer
        // conferência manual da documentação pela equipe.
        if (notEmpty(req.tradeName())) logReadOnly("tradeName");
        if (notEmpty(req.legalName())) logReadOnly("legalName");
        if (notEmpty(req.cnpj())) logReadOnly("cnpj");
        if (notEmpty(req.partnerOwnerName())) logReadOnly("partnerOwnerName");
        if (notEmpty(req.partnerOwnerCpf())) logReadOnly("partnerOwnerCpf");

        if (req.description() != null) t.setDescription(req.description());
        if (req.phone() != null) t.setPhone(req.phone());
        if (req.whatsapp() != null) t.setWhatsapp(req.whatsapp());
        if (req.adminPhone() != null) t.setAdminPhone(req.adminPhone());
        if (req.email() != null) t.setEmail(req.email());
        if (req.website() != null) t.setWebsite(req.website());
        if (req.logoUrl() != null) t.setLogoUrl(req.logoUrl());
        if (req.bannerUrl() != null) t.setBannerUrl(req.bannerUrl());

        if (req.addressZipCode() != null) t.setAddressZipCode(req.addressZipCode());
        if (req.addressStreet() != null) t.setAddressStreet(req.addressStreet());
        if (req.addressNumber() != null) t.setAddressNumber(req.addressNumber());
        if (req.addressComplement() != null) t.setAddressComplement(req.addressComplement());
        if (req.addressNeighborhood() != null) t.setAddressNeighborhood(req.addressNeighborhood());
        if (req.addressCity() != null) t.setAddressCity(req.addressCity());
        if (req.addressState() != null) t.setAddressState(req.addressState());

        Double lat = req.addressLatitude();
        Double lng = req.addressLongitude();
        if ((lat == null || lng == null) && req.addressZipCode() != null && !req.addressZipCode().isBlank()) {
            CepLookupView geo = cepLookupService.lookup(req.addressZipCode());
            if (geo != null) {
                if (lat == null) lat = geo.latitude();
                if (lng == null) lng = geo.longitude();
                // Se o usuário não preencheu logradouro/cidade/UF, usa o do CepAberto.
                if (req.addressStreet() == null && geo.street() != null) t.setAddressStreet(geo.street());
                if (req.addressNeighborhood() == null && geo.neighborhood() != null) t.setAddressNeighborhood(geo.neighborhood());
                if (req.addressCity() == null && geo.city() != null) t.setAddressCity(geo.city());
                if (req.addressState() == null && geo.state() != null) t.setAddressState(geo.state());
            }
        }
        if (lat != null && lng != null) {
            t.setAddressLocation(GEOMETRY.createPoint(new Coordinate(lng, lat)));
        }
        tenantRepository.save(t);
        return br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView.of(t);
    }

    @GetMapping("/custom-domain")
    public CustomDomainView getCustomDomain() {
        Tenant t = load();
        return new CustomDomainView(
            t.getCustomDomain(),
            t.getCustomDomainStatus() == null ? "NONE" : t.getCustomDomainStatus().name(),
            t.getCustomDomainLastCheckAt(),
            t.getCustomDomainError(),
            customDomainService.platformCname(t.getId())
        );
    }

    @PostMapping("/custom-domain")
    public CustomDomainView setCustomDomain(@RequestBody SetCustomDomainRequest req) {
        Tenant t = customDomainService.setCustomDomain(TenantContextHolder.requireId(), req.domain());
        return new CustomDomainView(
            t.getCustomDomain(),
            t.getCustomDomainStatus().name(),
            t.getCustomDomainLastCheckAt(),
            t.getCustomDomainError(),
            customDomainService.platformCname(t.getId())
        );
    }

    @PostMapping("/custom-domain/verify")
    public CustomDomainCheck verify() {
        return customDomainService.verify(TenantContextHolder.requireId());
    }

    @GetMapping("/custom-domain/history")
    public List<CustomDomainCheck> history() {
        return customDomainService.history(TenantContextHolder.requireId());
    }

    private Tenant load() {
        UUID id = TenantContextHolder.requireId();
        return tenantRepository.findById(id).orElseThrow(() -> TenantExceptions.notFound("Tenant"));
    }

    private br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView toView(Tenant t) {
        return br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView.of(t);
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isBlank();
    }

    private static void logReadOnly(String field) {
        log.warn("Campo '{}' é somente leitura no perfil do tenant. Edição requer suporte manual.", field);
    }

    public record UpdateProfileRequest(
        String tradeName, String legalName, String cnpj,
        String partnerOwnerName, String partnerOwnerCpf,
        String description,
        String phone, String whatsapp, String adminPhone, String email, String website,
        String logoUrl, String bannerUrl,
        String addressZipCode, String addressStreet, String addressNumber,
        String addressComplement, String addressNeighborhood, String addressCity, String addressState,
        Double addressLatitude, Double addressLongitude
    ) {}

    public record SetCustomDomainRequest(String domain) {}

    public record CustomDomainView(
        String domain,
        String status,
        OffsetDateTime lastCheckAt,
        String lastError,
        String expectedCname
    ) {}
}
