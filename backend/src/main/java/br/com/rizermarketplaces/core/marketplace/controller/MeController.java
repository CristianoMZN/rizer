package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.dto.AddressRequest;
import br.com.rizermarketplaces.core.marketplace.dto.AddressView;
import br.com.rizermarketplaces.core.marketplace.dto.ConsumerProfileView;
import br.com.rizermarketplaces.core.marketplace.dto.FavoriteView;
import br.com.rizermarketplaces.core.marketplace.dto.UpdateMyProfileRequest;
import br.com.rizermarketplaces.core.marketplace.partner.PublicPartnerService;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.service.MeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/me")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me", description = "Perfil, endereços e favoritos do usuário autenticado")
public class MeController {

    private final MeService meService;
    private final PublicPartnerService partnerService;
    private final ProductRepository productRepository;

    public MeController(
        MeService meService,
        PublicPartnerService partnerService,
        ProductRepository productRepository
    ) {
        this.meService = meService;
        this.partnerService = partnerService;
        this.productRepository = productRepository;
    }

    // ─── Perfil ─────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ConsumerProfileView getProfile() {
        return meService.getProfile(CurrentUser.require().getId());
    }

    @PatchMapping("/profile")
    public ConsumerProfileView updateProfile(@RequestBody @Valid UpdateMyProfileRequest body) {
        return meService.updateProfile(CurrentUser.require().getId(), body);
    }

    // ─── Endereços ──────────────────────────────────────────────────────

    @GetMapping("/addresses")
    public List<AddressView> listAddresses() {
        return meService.listAddresses(CurrentUser.require().getId());
    }

    @PostMapping("/addresses")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public AddressView createAddress(@RequestBody @Valid AddressRequest body) {
        return meService.createAddress(CurrentUser.require().getId(), body);
    }

    @PatchMapping("/addresses/{id}")
    public AddressView updateAddress(@PathVariable UUID id, @RequestBody @Valid AddressRequest body) {
        return meService.updateAddress(CurrentUser.require().getId(), id, body);
    }

    @DeleteMapping("/addresses/{id}")
    public Map<String, String> deleteAddress(@PathVariable UUID id) {
        meService.deleteAddress(CurrentUser.require().getId(), id);
        return Map.of("status", "deleted");
    }

    @PostMapping("/addresses/{id}/primary")
    public AddressView setPrimary(@PathVariable UUID id) {
        return meService.setPrimary(CurrentUser.require().getId(), id);
    }

    // ─── Favoritos ──────────────────────────────────────────────────────

    @GetMapping("/favorites")
    public List<FavoriteView> listFavorites() {
        return meService.listFavorites(CurrentUser.require().getId(), productId ->
            productRepository.findById(productId)
                .map(partnerService::toPublicProduct)
                .orElse(null)
        );
    }

    @GetMapping("/favorites/ids")
    public FavoriteView.IdList listFavoriteIds() {
        return new FavoriteView.IdList(meService.listFavoriteIds(CurrentUser.require().getId()));
    }

    @PostMapping("/favorites/{productId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> addFavorite(@PathVariable UUID productId) {
        if (productRepository.findById(productId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado");
        }
        meService.addFavorite(CurrentUser.require().getId(), productId);
        return Map.of("status", "favorited");
    }

    @DeleteMapping("/favorites/{productId}")
    public Map<String, String> removeFavorite(@PathVariable UUID productId) {
        meService.removeFavorite(CurrentUser.require().getId(), productId);
        return Map.of("status", "unfavorited");
    }
}
