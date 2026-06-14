package br.com.rizermarketplaces.core.marketplace.audit;

import br.com.rizermarketplaces.core.marketplace.auth.AuthenticatedUser;
import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.tools.RequestUtils;
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
                e.setIp(RequestUtils.extractIp(request));
                e.setUserAgent(request.getHeader("User-Agent"));
            }
            repository.save(e);
        } catch (Exception ex) {
            // Auditoria nunca deve quebrar a request principal
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
