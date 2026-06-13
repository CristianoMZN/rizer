package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionService;
import br.com.rizermarketplaces.core.marketplace.billing.TrialService;
import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.SubscriptionView;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/tenant/billing/trial")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Trial", description = "Iniciar período de trial")
public class TenantTrialController {

    private final TrialService trialService;
    private final SubscriptionService subscriptionService;

    public TenantTrialController(TrialService trialService, SubscriptionService subscriptionService) {
        this.trialService = trialService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/{planCode}")
    public SubscriptionView start(@PathVariable String planCode) {
        UUID tenantId = TenantContextHolder.getId();
        if (tenantId == null) throw TenantExceptions.forbidden("Selecione um tenant antes de continuar");
        trialService.startTrial(tenantId, planCode);
        return subscriptionService.getView(tenantId);
    }
}
