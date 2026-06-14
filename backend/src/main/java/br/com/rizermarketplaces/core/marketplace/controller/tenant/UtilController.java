package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.CepLookupView;
import br.com.rizermarketplaces.core.marketplace.tenant.CepLookupService;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/util")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Utilitários", description = "Endpoints auxiliares (CEP, etc.)")
public class UtilController {

    private final CepLookupService cepLookupService;

    public UtilController(CepLookupService cepLookupService) {
        this.cepLookupService = cepLookupService;
    }

    @GetMapping("/cep/{cep}")
    public ResponseEntity<CepLookupView> lookupCep(@PathVariable String cep) {
        if (cep == null || cep.replaceAll("\\D", "").length() != 8) {
            throw TenantExceptions.badRequest("CEP inválido. Use 8 dígitos.");
        }
        CepLookupView v = cepLookupService.lookup(cep);
        if (v == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(v);
    }
}
