package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.model.CustomDomainCheck;
import br.com.rizermarketplaces.core.marketplace.model.CustomDomainStatus;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.CustomDomainService;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final TenantRepository tenantRepository;
    private final CustomDomainService customDomainService;

    public TenantSettingsController(
        TenantRepository tenantRepository,
        CustomDomainService customDomainService
    ) {
        this.tenantRepository = tenantRepository;
        this.customDomainService = customDomainService;
    }

    @GetMapping
    public br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView get() {
        Tenant t = load();
        return br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView.of(t);
    }

    @PostMapping("/profile")
    public br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView updateProfile(@RequestBody UpdateProfileRequest req) {
        Tenant t = load();
        if (req.tradeName() != null && !req.tradeName().isBlank()) t.setTradeName(req.tradeName());
        if (req.legalName() != null) t.setLegalName(req.legalName());
        if (req.description() != null) t.setDescription(req.description());
        if (req.phone() != null) t.setPhone(req.phone());
        if (req.whatsapp() != null) t.setWhatsapp(req.whatsapp());
        if (req.email() != null) t.setEmail(req.email());
        if (req.website() != null) t.setWebsite(req.website());
        if (req.logoUrl() != null) t.setLogoUrl(req.logoUrl());
        if (req.bannerUrl() != null) t.setBannerUrl(req.bannerUrl());
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
        Tenant t = customDomainService.setCustomDomain(requireTenant(), req.domain());
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
        return customDomainService.verify(requireTenant());
    }

    @GetMapping("/custom-domain/history")
    public List<CustomDomainCheck> history() {
        return customDomainService.history(requireTenant());
    }

    private Tenant load() {
        UUID id = requireTenant();
        return tenantRepository.findById(id).orElseThrow(() -> TenantExceptions.notFound("Tenant"));
    }

    private UUID requireTenant() {
        UUID id = TenantContextHolder.getId();
        if (id == null) throw TenantExceptions.forbidden("Selecione um tenant antes de continuar");
        return id;
    }

    private br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView toView(Tenant t) {
        return br.com.rizermarketplaces.core.marketplace.dto.TenantSettingsView.of(t);
    }

    public record UpdateProfileRequest(
        String tradeName, String legalName, String description,
        String phone, String whatsapp, String email, String website,
        String logoUrl, String bannerUrl
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
