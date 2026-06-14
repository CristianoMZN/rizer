package br.com.rizermarketplaces.core.marketplace.controller.publicapi;

import br.com.rizermarketplaces.core.marketplace.lead.LeadDtos.CreateLeadRequest;
import br.com.rizermarketplaces.core.marketplace.lead.LeadDtos.LeadView;
import br.com.rizermarketplaces.core.marketplace.lead.LeadService;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/{countryCode}/public/leads")
@Tag(name = "Público · Leads", description = "Captura de leads (interesse em veículos)")
public class PublicLeadController {

    private final LeadService leadService;
    private final ProductRepository productRepository;

    public PublicLeadController(LeadService leadService, ProductRepository productRepository) {
        this.leadService = leadService;
        this.productRepository = productRepository;
    }

    @PostMapping
    public ResponseEntity<LeadView> create(
        @PathVariable String countryCode,
        @Valid @RequestBody CreateLeadRequest req,
        HttpServletRequest request
    ) {
        UUID tenantId = null;
        UUID storeId = req.storeId();
        UUID productId = req.productId();

        if (productId != null) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> TenantExceptions.notFound("Produto não encontrado"));
            tenantId = product.getTenantId();
            if (storeId == null) {
                storeId = product.getPhysicalStoreId();
            }
        }

        if (tenantId == null) {
            throw TenantExceptions.badRequest("Não foi possível identificar o anunciante");
        }

        var lead = leadService.create(
            tenantId, productId, storeId,
            req.buyerName(), req.buyerEmail(), req.buyerPhone(),
            req.message(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.toView(lead));
    }
}
