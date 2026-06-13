package br.com.rizermarketplaces.core.marketplace.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Stub para integração com a Cloudflare API.
 *
 * Em prod:
 *  - provisionar o subdomínio `slug.motorise.com.br` na zona `motorise.com.br`
 *    (DNS A ou CNAME para o load balancer da Motorise, com proxy habilitado).
 *  - emitir/renovar cert SSL via Cloudflare Origin CA quando o tenant
 *    tem custom_domain_status = VERIFIED.
 *  - opcionalmente, configurar Page Rules para redirecionar `www.custom`
 *    → `custom` (apex → www ou vice-versa).
 *
 * Dependência em prod: implementação via OkHttp/Feign chamando
 * https://api.cloudflare.com/client/v4/zones/{id}/dns_records com
 * Authorization: Bearer {CLOUDFLARE_API_TOKEN}.
 */
@Service
public class CloudflareService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareService.class);

    @Value("${app.cloudflare.api-token:}")
    private String apiToken;

    @Value("${app.cloudflare.zone-id:}")
    private String zoneId;

    @Value("${app.cloudflare.enabled:false}")
    private boolean enabled;

    /**
     * Provisiona o subdomínio `slug.motorise.com.br` apontando para o
     * load balancer da plataforma.
     */
    public void provisionPlatformSubdomain(String slug, String lbTarget) {
        if (!enabled) {
            log.warn("[cloudflare-mock] provisionPlatformSubdomain slug={} target={}", slug, lbTarget);
            return;
        }
        // TODO(fase-7-prod): POST /zones/{zoneId}/dns_records
    }

    /**
     * Emite/renova o certificado SSL para o custom domain.
     * Em dev, marca como pendente com TODO para a fase de produção.
     */
    public Map<String, Object> issueOrRenewSsl(UUID tenantId, String domain) {
        if (!enabled) {
            log.warn("[cloudflare-mock] issueOrRenewSsl tenant={} domain={}", tenantId, domain);
            return Map.of("status", "PENDING", "note", "SSL não emitido em dev — TODO(fase-7-prod)");
        }
        // TODO(fase-7-prod): POST /zones/{zoneId}/ssl/certificate_packs
        return Map.of("status", "ISSUED");
    }
}
