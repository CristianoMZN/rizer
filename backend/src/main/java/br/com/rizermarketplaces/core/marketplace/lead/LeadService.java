package br.com.rizermarketplaces.core.marketplace.lead;

import br.com.rizermarketplaces.core.marketplace.model.Lead;
import br.com.rizermarketplaces.core.marketplace.model.LeadStatus;
import br.com.rizermarketplaces.core.marketplace.repository.LeadRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import br.com.rizermarketplaces.core.marketplace.tools.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import br.com.rizermarketplaces.core.marketplace.lead.LeadDtos.LeadView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LeadService {

    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Transactional
    public Lead create(UUID tenantId, UUID productId, UUID storeId,
                       String buyerName, String buyerEmail, String buyerPhone,
                       String message, HttpServletRequest request) {
        Lead lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setProductId(productId);
        lead.setPhysicalStoreId(storeId);
        lead.setBuyerName(buyerName);
        lead.setBuyerEmail(buyerEmail);
        lead.setBuyerPhone(buyerPhone);
        lead.setMessage(message);
        lead.setStatus(LeadStatus.NEW);
        if (request != null) {
            lead.setIp(RequestUtils.extractIp(request));
            lead.setUserAgent(request.getHeader("User-Agent"));
        }
        return leadRepository.save(lead);
    }

    @Transactional(readOnly = true)
    public List<Lead> listByTenant(UUID tenantId) {
        return leadRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public Lead updateStatus(UUID tenantId, UUID leadId, LeadStatus newStatus) {
        Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> TenantExceptions.notFound("Lead não encontrado"));
        if (!lead.getTenantId().equals(tenantId)) {
            throw TenantExceptions.forbidden("Lead não pertence a este tenant");
        }
        lead.setStatus(newStatus);
        return leadRepository.save(lead);
    }

    public LeadView toView(Lead lead) {
        return new LeadDtos.LeadView(
            lead.getId(),
            lead.getTenantId(),
            lead.getProductId(),
            lead.getPhysicalStoreId(),
            lead.getBuyerName(),
            lead.getBuyerEmail(),
            lead.getBuyerPhone(),
            lead.getMessage(),
            lead.getStatus().name(),
            lead.getSellerUserId(),
            lead.getCreatedAt(),
            lead.getUpdatedAt()
        );
    }

}
