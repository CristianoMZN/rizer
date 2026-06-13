package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionStateMachine;
import br.com.rizermarketplaces.core.marketplace.model.CustomDomainCheck;
import br.com.rizermarketplaces.core.marketplace.model.CustomDomainStatus;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.repository.CustomDomainCheckRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gerencia o custom domain (CNAME → slug.motorise.com.br).
 *
 * Restrito a tenants com plano PRO/PLATINUM (feature.has_custom_domain).
 * SSL é **TODO(fase-7-prod)**: emitido via Cloudflare Origin CA quando o
 * tenant estiver em produção e o domínio estiver VERIFIED. Por enquanto
 * a aplicação responde via Cloudflare proxy ou Caddy on-demand TLS.
 */
@Service
public class CustomDomainService {

    private static final Logger log = LoggerFactory.getLogger(CustomDomainService.class);
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
        "^(?=.{1,253}$)([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)+$"
    );

    private final TenantRepository tenantRepository;
    private final CustomDomainCheckRepository checkRepository;
    private final DnsLookupService dnsLookupService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionStateMachine stateMachine;

    @Value("${app.tenant.platform-domain:motorise.com.br}")
    private String platformDomain;

    public CustomDomainService(
        TenantRepository tenantRepository,
        CustomDomainCheckRepository checkRepository,
        DnsLookupService dnsLookupService,
        SubscriptionService subscriptionService,
        SubscriptionStateMachine stateMachine
    ) {
        this.tenantRepository = tenantRepository;
        this.checkRepository = checkRepository;
        this.dnsLookupService = dnsLookupService;
        this.subscriptionService = subscriptionService;
        this.stateMachine = stateMachine;
    }

    public String platformCname(UUID tenantId) {
        Tenant t = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        return t.getSlug() + "." + platformDomain;
    }

    @Transactional
    public Tenant setCustomDomain(UUID tenantId, String domain) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        requireCustomDomainFeature(tenant);

        if (domain == null || domain.isBlank()) {
            // Limpa
            tenant.setCustomDomain(null);
            tenant.setCustomDomainStatus(CustomDomainStatus.NONE);
            tenant.setCustomDomainError(null);
            tenant.setCustomDomainLastCheckAt(null);
            return tenantRepository.save(tenant);
        }

        String normalized = normalize(domain);
        if (!DOMAIN_PATTERN.matcher(normalized).matches()) {
            throw TenantExceptions.badRequest("Domínio inválido: " + normalized);
        }
        if (normalized.equalsIgnoreCase(platformDomain) || normalized.endsWith("." + platformDomain)) {
            throw TenantExceptions.badRequest("Não é possível usar um subdomínio de " + platformDomain);
        }
        if (tenantRepository.findByCustomDomainIgnoreCaseAndDeletedAtIsNull(normalized).isPresent()) {
            throw TenantExceptions.conflict("Este domínio já está em uso por outro tenant");
        }

        tenant.setCustomDomain(normalized);
        tenant.setCustomDomainStatus(CustomDomainStatus.PENDING);
        tenant.setCustomDomainError(null);
        tenant.setCustomDomainLastCheckAt(null);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public CustomDomainCheck verify(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        if (tenant.getCustomDomain() == null) {
            throw TenantExceptions.badRequest("Nenhum domínio configurado");
        }
        requireCustomDomainFeature(tenant);

        String expected = platformCname(tenantId);
        DnsLookupService.DnsResult result = dnsLookupService.lookup(tenant.getCustomDomain());

        CustomDomainCheck check = new CustomDomainCheck();
        check.setTenantId(tenantId);
        check.setDomain(tenant.getCustomDomain());
        check.setExpectedTarget(expected);
        check.setCnameFound(result.cnameTarget);
        check.setResolvedIp(result.resolvedIp);

        boolean ok = result.ok()
            && (result.cnameTarget != null && result.cnameTarget.equalsIgnoreCase(expected));
        if (ok) {
            check.setStatus("VERIFIED");
            tenant.setCustomDomainStatus(CustomDomainStatus.VERIFIED);
            tenant.setCustomDomainError(null);
            // TODO(fase-7-prod): disparar emissão/renovação de SSL via Cloudflare Origin CA
        } else {
            String reason = result.ok()
                ? "CNAME aponta para " + result.cnameTarget + " (esperado " + expected + ")"
                : result.error;
            check.setStatus("FAILED");
            check.setErrorMessage(reason);
            tenant.setCustomDomainStatus(CustomDomainStatus.FAILED);
            tenant.setCustomDomainError(reason);
        }
        tenant.setCustomDomainLastCheckAt(OffsetDateTime.now());
        tenantRepository.save(tenant);
        checkRepository.save(check);
        log.info("[custom-domain] tenant={} domain={} status={} cname={} ip={}",
            tenantId, tenant.getCustomDomain(), check.getStatus(), result.cnameTarget, result.resolvedIp);
        return check;
    }

    @Transactional(readOnly = true)
    public List<CustomDomainCheck> history(UUID tenantId) {
        return checkRepository.findTop10ByTenantIdOrderByCheckedAtDesc(tenantId);
    }

    private void requireCustomDomainFeature(Tenant tenant) {
        Subscription sub = subscriptionService.getEntity(tenant.getId())
            .orElseThrow(() -> TenantExceptions.paymentRequired("Sem assinatura ativa"));
        if (!stateMachine.isActiveLike(sub)) {
            throw TenantExceptions.paymentRequired("Assinatura inativa — regularize para usar domínio customizado.");
        }
        // Feature flag do plano (has_custom_domain). PRO+ têm.
        var planOpt = subscriptionService.getEntity(tenant.getId());
        if (planOpt.isEmpty()) {
            throw TenantExceptions.paymentRequired("Sem assinatura ativa");
        }
        String planCode = planOpt.get().getPlanCode();
        if (!"PRO".equals(planCode) && !"PLATINUM".equals(planCode)) {
            throw TenantExceptions.paymentRequired("Domínio customizado requer plano PRO ou Platinum. Plano atual: " + planCode);
        }
    }

    private String normalize(String domain) {
        String d = domain.trim().toLowerCase();
        if (d.startsWith("http://")) d = d.substring(7);
        else if (d.startsWith("https://")) d = d.substring(8);
        if (d.endsWith("/")) d = d.substring(0, d.length() - 1);
        return d;
    }
}
