package br.com.rizermarketplaces.core.marketplace.controller.publicapi;

import br.com.rizermarketplaces.core.marketplace.billing.PlanService;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PlanView;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/{countryCode}/public/billing")
@Tag(name = "Público · Planos", description = "Lista de planos comercializáveis")
public class PublicPlansController {

    private final PlanService planService;

    public PublicPlansController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/plans")
    public List<PlanView> list(@PathVariable String countryCode) {
        // TODO(fase-5-multi-currency): filtrar por country.currency_code_iso
        return planService.listActive();
    }
}
