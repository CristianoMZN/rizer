package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.lead.LeadDtos.LeadView;
import br.com.rizermarketplaces.core.marketplace.lead.LeadDtos.UpdateStatusRequest;
import br.com.rizermarketplaces.core.marketplace.lead.LeadService;
import br.com.rizermarketplaces.core.marketplace.model.LeadStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tenant/leads")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Leads", description = "Leads recebidos pelo tenant")
public class TenantLeadController {

    private final LeadService leadService;

    public TenantLeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public List<LeadView> list() {
        UUID tenantId = TenantContextHolder.requireId();
        return leadService.listByTenant(tenantId).stream()
            .map(leadService::toView)
            .toList();
    }

    @PatchMapping("/{id}/status")
    public LeadView updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest req) {
        UUID tenantId = TenantContextHolder.requireId();
        LeadStatus status;
        try {
            status = LeadStatus.valueOf(req.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + req.status());
        }
        var lead = leadService.updateStatus(tenantId, id, status);
        return leadService.toView(lead);
    }

}
