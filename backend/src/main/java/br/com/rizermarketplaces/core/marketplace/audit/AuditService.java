package br.com.rizermarketplaces.core.marketplace.audit;

import br.com.rizermarketplaces.core.marketplace.auth.AuthenticatedUser;
import br.com.rizermarketplaces.core.marketplace.context.CountryContextHolder;
import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Auditoria centralizada. Ações sensíveis devem chamar {@link #record(...)}
 * explicitamente. Em Fase 9 usamos @Audited explícito (sem AOP) para
 * manter a rastreabilidade clara no código.
 *
 * A transação é REQUIRES_NEW para que a auditoria seja gravada mesmo
 * se a transação principal der rollback.
 */
@Service
public class AuditService {

    private final AuditEntryRepository repository;

    public AuditService(AuditEntryRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, String resourceId, Map<String, Object> payload) {
        record(action, resourceType, resourceId, AuditSeverity.INFO, payload);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, String resourceId,
                       AuditSeverity severity, Map<String, Object> payload) {
        try {
            AuditEntry e = new AuditEntry();
            var principal = br.com.rizermarketplaces.core.marketplace.auth.CurrentUser.orNull();
            if (principal != null) e.setActorUserId(principal.getId());
            UUID tid = TenantContextHolder.getId();
            if (tid != null) e.setTenantId(tid);
            e.setAction(action);
            e.setResourceType(resourceType);
            e.setResourceId(resourceId);
            e.setSeverity(severity);
            e.setPayload(payload == null ? new HashMap<>() : payload);
            e.setCorrelationId(MDC.get("correlationId"));
            HttpServletRequest request = currentRequest();
            if (request != null) {
                String fwd = request.getHeader("X-Forwarded-For");
                if (fwd != null && !fwd.isBlank()) {
                    int comma = fwd.indexOf(',');
                    e.setIp(comma >= 0 ? fwd.substring(0, comma).trim() : fwd.trim());
                } else {
                    e.setIp(request.getRemoteAddr());
                }
                e.setUserAgent(request.getHeader("User-Agent"));
            }
            repository.save(e);
        } catch (Exception ex) {
            // Auditoria nunca deve quebrar a request principal
            org.slf4j.LoggerFactory.getLogger(AuditService.class)
                .warn("[audit] falha ao gravar ação {}: {}", action, ex.getMessage());
        }
    }

    public void recordFromActor(AuthenticatedUser actor, UUID tenantId, String action,
                                String resourceType, String resourceId, Map<String, Object> payload) {
        try {
            AuditEntry e = new AuditEntry();
            if (actor != null) e.setActorUserId(actor.getId());
            e.setTenantId(tenantId);
            e.setAction(action);
            e.setResourceType(resourceType);
            e.setResourceId(resourceId);
            e.setSeverity(AuditSeverity.INFO);
            e.setPayload(payload == null ? new HashMap<>() : payload);
            e.setCorrelationId(MDC.get("correlationId"));
            e.setIp(CountryContextHolder.get() != null ? CountryContextHolder.get() : null);
            repository.save(e);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(AuditService.class)
                .warn("[audit] falha ao gravar ação {}: {}", action, ex.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
