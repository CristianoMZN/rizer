package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.IntegrationDtos.AuthorizeResponse;
import br.com.rizermarketplaces.core.marketplace.dto.IntegrationDtos.IntegrationView;
import br.com.rizermarketplaces.core.marketplace.dto.IntegrationDtos.OAuthCallbackRequest;
import br.com.rizermarketplaces.core.marketplace.integration.InstagramService;
import br.com.rizermarketplaces.core.marketplace.model.IntegrationProvider;
import br.com.rizermarketplaces.core.marketplace.model.TenantIntegration;
import br.com.rizermarketplaces.core.marketplace.repository.TenantIntegrationRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tenant/integrations")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Integrações", description = "Conexão e gestão de Instagram, META Business e Google Merchant")
public class TenantIntegrationController {

    private final InstagramService instagramService;
    private final TenantIntegrationRepository integrationRepository;

    public TenantIntegrationController(
        InstagramService instagramService,
        TenantIntegrationRepository integrationRepository
    ) {
        this.instagramService = instagramService;
        this.integrationRepository = integrationRepository;
    }

    @GetMapping
    public List<IntegrationView> list() {
        return integrationRepository.findAllByTenantIdOrderByProviderAsc(requireTenant())
            .stream().map(instagramService::toView).toList();
    }

    @GetMapping("/{provider}/authorize")
    public AuthorizeResponse authorize(@PathVariable("provider") String providerStr) {
        IntegrationProvider provider = parseProvider(providerStr);
        if (provider == IntegrationProvider.INSTAGRAM) {
            return new AuthorizeResponse(instagramService.buildAuthorizeUrl(requireTenant()), null);
        }
        if (provider == IntegrationProvider.META_BUSINESS) {
            // Por enquanto, fluxo igual ao Instagram; em prod, scopes diferentes
            return new AuthorizeResponse(instagramService.buildAuthorizeUrl(requireTenant()), null);
        }
        if (provider == IntegrationProvider.GOOGLE_MERCHANT) {
            // TODO(fase-6-google): implementar OAuth Google Content API
            throw TenantExceptions.badRequest("Google Merchant: fluxo ainda não implementado nesta fase. Use o feed XML público.");
        }
        throw TenantExceptions.badRequest("Provider inválido: " + providerStr);
    }

    @PostMapping("/{provider}/callback")
    public ResponseEntity<IntegrationView> callback(
        @PathVariable("provider") String providerStr,
        @RequestBody OAuthCallbackRequest body
    ) throws Exception {
        IntegrationProvider provider = parseProvider(providerStr);
        UUID tenantId = requireTenant();
        if (provider == IntegrationProvider.INSTAGRAM || provider == IntegrationProvider.META_BUSINESS) {
            return ResponseEntity.ok(instagramService.completeOAuth(tenantId, body));
        }
        throw TenantExceptions.badRequest("Provider não suporta callback: " + provider);
    }

    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> disconnect(@PathVariable("provider") String providerStr) {
        IntegrationProvider provider = parseProvider(providerStr);
        if (provider == IntegrationProvider.INSTAGRAM || provider == IntegrationProvider.META_BUSINESS) {
            instagramService.disconnect(requireTenant());
            return ResponseEntity.noContent().build();
        }
        throw TenantExceptions.badRequest("Provider não suporta disconnect: " + provider);
    }

    @PostMapping("/instagram/publish/{productId}")
    public java.util.Map<String, Object> publishToInstagram(@PathVariable UUID productId) {
        String mediaId = instagramService.publishProduct(requireTenant(), productId);
        return java.util.Map.of("mediaId", mediaId);
    }

    private IntegrationProvider parseProvider(String s) {
        try { return IntegrationProvider.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw TenantExceptions.badRequest("Provider inválido: " + s);
        }
    }

    private UUID requireTenant() {
        UUID id = TenantContextHolder.getId();
        if (id == null) throw TenantExceptions.forbidden("Selecione um tenant antes de continuar");
        return id;
    }
}
