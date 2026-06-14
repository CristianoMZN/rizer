package br.com.rizermarketplaces.core.marketplace.service;

import br.com.rizermarketplaces.core.marketplace.audit.AuditService;
import br.com.rizermarketplaces.core.marketplace.dto.AddressRequest;
import br.com.rizermarketplaces.core.marketplace.dto.AddressView;
import br.com.rizermarketplaces.core.marketplace.dto.ConsumerProfileView;
import br.com.rizermarketplaces.core.marketplace.dto.FavoriteView;
import br.com.rizermarketplaces.core.marketplace.dto.PublicProductView;
import br.com.rizermarketplaces.core.marketplace.dto.UpdateMyProfileRequest;
import br.com.rizermarketplaces.core.marketplace.model.Address;
import br.com.rizermarketplaces.core.marketplace.model.Favorite;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.AddressRepository;
import br.com.rizermarketplaces.core.marketplace.repository.FavoriteRepository;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import br.com.rizermarketplaces.core.marketplace.tools.CpfValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MeService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FavoriteRepository favoriteRepository;
    private final AuditService auditService;

    public MeService(
        UserRepository userRepository,
        AddressRepository addressRepository,
        FavoriteRepository favoriteRepository,
        AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.favoriteRepository = favoriteRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ConsumerProfileView getProfile(UUID userId) {
        User u = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> TenantExceptions.notFound("Usuário"));
        return ConsumerProfileView.from(u);
    }

    @Transactional
    public ConsumerProfileView updateProfile(UUID userId, UpdateMyProfileRequest req) {
        User u = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> TenantExceptions.notFound("Usuário"));

        if (req.getName() != null && !req.getName().isBlank()) {
            u.setName(req.getName().trim());
        }
        if (req.getPhone() != null) {
            u.setPhone(req.getPhone().trim().isEmpty() ? null : req.getPhone().trim());
        }
        if (req.getAvatarUrl() != null) {
            u.setAvatarUrl(req.getAvatarUrl().trim().isEmpty() ? null : req.getAvatarUrl().trim());
        }
        if (req.getBirthDate() != null) {
            u.setBirthDate(req.getBirthDate());
        }
        if (req.getCpf() != null) {
            String cpf = req.getCpf().trim();
            if (cpf.isEmpty()) {
                u.setCpf(null);
            } else {
                if (!CpfValidator.isValid(cpf)) {
                    throw TenantExceptions.badRequest("CPF inválido");
                }
                String normalized = CpfValidator.format(cpf);
                if (!normalized.equals(u.getCpf())) {
                    userRepository.findByCpfAndDeletedAtIsNull(normalized)
                        .ifPresent(other -> {
                            if (!other.getId().equals(u.getId())) {
                                throw TenantExceptions.conflict("CPF já cadastrado");
                            }
                        });
                }
                u.setCpf(normalized);
            }
        }

        u.setProfileCompleted(true);
        userRepository.save(u);
        auditService.record("user.profile.updated", "user", u.getId().toString(),
            null, Map.of("fields", List.of("name", "phone", "cpf", "birthDate", "avatarUrl").toString()));
        return ConsumerProfileView.from(u);
    }

    @Transactional(readOnly = true)
    public List<AddressView> listAddresses(UUID userId) {
        return addressRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .map(AddressView::from)
            .toList();
    }

    @Transactional
    public AddressView createAddress(UUID userId, AddressRequest req) {
        Address a = new Address();
        a.setUserId(userId);
        applyRequest(a, req);
        if (Boolean.TRUE.equals(req.getIsPrimary())) {
            clearPrimary(userId);
            a.setPrimary(true);
        }
        addressRepository.save(a);
        return AddressView.from(a);
    }

    @Transactional
    public AddressView updateAddress(UUID userId, UUID addressId, AddressRequest req) {
        Address a = addressRepository.findById(addressId)
            .filter(addr -> addr.getUserId() != null && addr.getUserId().equals(userId))
            .filter(addr -> addr.getDeletedAt() == null)
            .orElseThrow(() -> TenantExceptions.notFound("Endereço"));
        applyRequest(a, req);
        if (Boolean.TRUE.equals(req.getIsPrimary())) {
            clearPrimary(userId);
            a.setPrimary(true);
        }
        addressRepository.save(a);
        return AddressView.from(a);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address a = addressRepository.findById(addressId)
            .filter(addr -> addr.getUserId() != null && addr.getUserId().equals(userId))
            .filter(addr -> addr.getDeletedAt() == null)
            .orElseThrow(() -> TenantExceptions.notFound("Endereço"));
        a.setDeletedAt(OffsetDateTime.now());
        addressRepository.save(a);
    }

    @Transactional
    public AddressView setPrimary(UUID userId, UUID addressId) {
        Address a = addressRepository.findById(addressId)
            .filter(addr -> addr.getUserId() != null && addr.getUserId().equals(userId))
            .filter(addr -> addr.getDeletedAt() == null)
            .orElseThrow(() -> TenantExceptions.notFound("Endereço"));
        clearPrimary(userId);
        a.setPrimary(true);
        addressRepository.save(a);
        return AddressView.from(a);
    }

    private void applyRequest(Address a, AddressRequest req) {
        if (req.getLabel() != null) a.setLabel(req.getLabel());
        if (req.getZipCode() != null) a.setZipCode(req.getZipCode());
        if (req.getStreet() != null) a.setStreet(req.getStreet());
        if (req.getNumber() != null) a.setNumber(req.getNumber());
        if (req.getComplement() != null) a.setComplement(req.getComplement());
        if (req.getNeighborhood() != null) a.setNeighborhood(req.getNeighborhood());
        if (req.getCity() != null) a.setCity(req.getCity());
        if (req.getState() != null) a.setState(req.getState());
        if (req.getCountryCode() != null) a.setCountryCode(req.getCountryCode().toUpperCase());
        if (req.getCountry() != null) a.setCountry(req.getCountry());
    }

    private void clearPrimary(UUID userId) {
        addressRepository.findByUserIdAndDeletedAtIsNull(userId).forEach(addr -> {
            if (addr.isPrimary()) {
                addr.setPrimary(false);
                addressRepository.save(addr);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<FavoriteView> listFavorites(UUID userId, ProductToView mapper) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(fav -> new FavoriteView(
                fav.getId(),
                fav.getProductId(),
                fav.getCreatedAt(),
                mapper.toView(fav.getProductId())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> listFavoriteIds(UUID userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(Favorite::getProductId)
            .toList();
    }

    @Transactional
    public void addFavorite(UUID userId, UUID productId) {
        favoriteRepository.findByUserIdAndProductId(userId, productId).ifPresentOrElse(
            existing -> { /* idempotente */ },
            () -> {
                Favorite f = new Favorite();
                f.setUserId(userId);
                f.setProductId(productId);
                favoriteRepository.save(f);
            }
        );
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID productId) {
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @FunctionalInterface
    public interface ProductToView {
        PublicProductView toView(UUID productId);
    }
}
