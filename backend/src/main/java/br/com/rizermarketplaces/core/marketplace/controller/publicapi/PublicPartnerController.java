package br.com.rizermarketplaces.core.marketplace.controller.publicapi;

import br.com.rizermarketplaces.core.marketplace.dto.PublicPartnerView;
import br.com.rizermarketplaces.core.marketplace.dto.PublicProductView;
import br.com.rizermarketplaces.core.marketplace.dto.PublicTenantView;
import br.com.rizermarketplaces.core.marketplace.partner.PublicPartnerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/{countryCode}/public/tenants")
@Tag(name = "Público · Parceiros", description = "Páginas de empresas parceiras")
public class PublicPartnerController {

    private final PublicPartnerService service;

    public PublicPartnerController(PublicPartnerService service) {
        this.service = service;
    }

    @GetMapping("/partner")
    public List<PublicPartnerView> listPartners(@PathVariable String countryCode) {
        return service.listPartners(countryCode.toUpperCase());
    }

    @GetMapping("/{slug}")
    public PublicTenantView getPartner(@PathVariable String slug) {
        return service.getTenant(slug);
    }

    @GetMapping("/{slug}/products")
    public List<PublicProductView> listProducts(
        @PathVariable String slug,
        @RequestParam(defaultValue = "60") int limit
    ) {
        return service.listTenantProducts(slug, limit);
    }
}
