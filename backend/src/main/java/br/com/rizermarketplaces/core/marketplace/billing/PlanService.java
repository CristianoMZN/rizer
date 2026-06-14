package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PlanView;
import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.repository.PlanRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanView> listActive() {
        return planRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
            .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public Plan get(String code) {
        return planRepository.findById(code)
            .orElseThrow(() -> TenantExceptions.badRequest("Plano inválido: " + code));
    }

    private boolean isUnlimited(Plan p) {
        return p.getMaxPhysicalStores() == null;
    }

    public void requireStoreSlot(Plan p, long currentActive) {
        if (isUnlimited(p)) return;
        if (currentActive >= p.getMaxPhysicalStores()) {
            throw TenantExceptions.paymentRequired(
                "Limite de " + p.getMaxPhysicalStores() + " lojas ativas atingido. Faça upgrade do plano."
            );
        }
    }

    private PlanView toView(Plan p) {
        return new PlanView(
            p.getCode(), p.getName(), p.getDescription(),
            p.getMaxPhysicalStores(),
            p.isHasPartnerPage(), p.isHasCustomDomain(),
            p.isHasInstagram(), p.isHasMetaDpa(), p.isHasGoogleShopping(),
            BigDecimal.valueOf(p.getPriceCents(), 2),
            p.getCurrency(),
            p.getTrialDays(),
            p.getSortOrder()
        );
    }
}
