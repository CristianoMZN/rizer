package br.com.rizermarketplaces.core.marketplace.admin;

import br.com.rizermarketplaces.core.marketplace.audit.AuditService;
import br.com.rizermarketplaces.core.marketplace.auth.AuthService;
import br.com.rizermarketplaces.core.marketplace.dto.CreateTenantRequest;
import br.com.rizermarketplaces.core.marketplace.dto.TenantView;
import br.com.rizermarketplaces.core.marketplace.dto.UpdateTenantRequest;
import br.com.rizermarketplaces.core.marketplace.model.Country;
import br.com.rizermarketplaces.core.marketplace.model.SystemRole;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.TenantStatus;
import br.com.rizermarketplaces.core.marketplace.model.TenantUser;
import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.CountryRepository;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantUserRepository;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantMapper;
import br.com.rizermarketplaces.core.marketplace.tools.CnpjValidator;
import br.com.rizermarketplaces.core.marketplace.tools.SlugGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final PhysicalStoreRepository physicalStoreRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditService auditService;

    public AdminTenantService(
        TenantRepository tenantRepository,
        TenantUserRepository tenantUserRepository,
        UserRepository userRepository,
        CountryRepository countryRepository,
        PhysicalStoreRepository physicalStoreRepository,
        PasswordEncoder passwordEncoder,
        AuthService authService,
        AuditService auditService
    ) {
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.userRepository = userRepository;
        this.countryRepository = countryRepository;
        this.physicalStoreRepository = physicalStoreRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditService = auditService;
    }

    @Transactional
    public TenantView createTenant(CreateTenantRequest req, UUID currentUserId) {
        Country country = countryRepository.findById(req.countryCode().toUpperCase())
            .orElseThrow(() -> TenantExceptions.badRequest("País inválido: " + req.countryCode()));

        String slug = SlugGenerator.from(req.slug());
        if (slug.isBlank()) throw TenantExceptions.badRequest("Slug inválido");
        if (tenantRepository.existsBySlugAndCountryCodeAndDeletedAtIsNull(slug, country.getCode())) {
            throw TenantExceptions.conflict("Já existe um tenant com este slug neste país");
        }
        if (req.cnpj() != null && !req.cnpj().isBlank() && !CnpjValidator.isValid(req.cnpj())) {
            throw TenantExceptions.badRequest("CNPJ inválido");
        }

        Tenant tenant = new Tenant();
        tenant.setCountryCode(country.getCode());
        tenant.setSlug(slug);
        tenant.setTradeName(req.tradeName().trim());
        tenant.setLegalName(req.legalName());
        tenant.setCnpj(req.cnpj() != null ? CnpjValidator.format(req.cnpj()) : null);
        tenant.setDescription(req.description());
        tenant.setPhone(req.phone());
        tenant.setWhatsapp(req.whatsapp());
        tenant.setEmail(req.email());
        tenant.setWebsite(req.website());
        tenant.setStatus(TenantStatus.active);
        tenant.setPublic(false);
        tenant.setPartnerPageEnabled(false);
        tenant.setHadTrial(req.startWithTrial());
        tenant.setCreatedByUserId(currentUserId);
        tenant.setTheme(defaultTheme());
        tenant = tenantRepository.save(tenant);

        // Cria ou anexa o 1º owner
        User owner = userRepository.findByEmail(req.ownerEmail().toLowerCase().trim())
            .orElseGet(() -> {
                User u = new User();
                u.setEmail(req.ownerEmail().toLowerCase().trim());
                u.setName(req.ownerName().trim());
                u.setPhone(req.ownerPhone());
                u.setPasswordHash(passwordEncoder.encode(req.ownerPassword()));
                u.setProvider("local");
                u.setSystemRole(SystemRole.agency_owner);
                return userRepository.save(u);
            });
        // Se o user já existia, promove o system_role para agency_owner se ainda não tinha
        if (owner.getSystemRole() == SystemRole.user) {
            owner.setSystemRole(SystemRole.agency_owner);
            userRepository.save(owner);
        }

        TenantUser link = new TenantUser();
        link.setTenantId(tenant.getId());
        link.setUserId(owner.getId());
        link.setRole(TenantUserRole.OWNER);
        link.setActive(true);
        link.setAcceptedAt(OffsetDateTime.now());
        link.setInvitedByUserId(currentUserId);
        tenantUserRepository.save(link);

        auditService.record("tenant.create", "tenant", tenant.getId().toString(),
            java.util.Map.of("slug", tenant.getSlug(), "tradeName", tenant.getTradeName(),
                "hadTrial", req.startWithTrial(), "ownerEmail", owner.getEmail()));

        return TenantMapper.toView(tenant, owner, 0, 1, List.of());
    }

    @Transactional(readOnly = true)
    public List<TenantView> listAll() {
        return tenantRepository.findAll().stream()
            .filter(t -> t.getDeletedAt() == null)
            .map(t -> {
                long stores = physicalStoreRepository.countByTenantIdAndIsActiveTrueAndDeletedAtIsNull(t.getId());
                long members = tenantUserRepository.countByTenantIdAndIsActiveTrue(t.getId());
                User owner = findOwner(t.getId());
                return TenantMapper.toView(t, owner, stores, members, List.of());
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public TenantView getById(UUID id) {
        Tenant t = tenantRepository.findById(id)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        long stores = physicalStoreRepository.countByTenantIdAndIsActiveTrueAndDeletedAtIsNull(t.getId());
        long members = tenantUserRepository.countByTenantIdAndIsActiveTrue(t.getId());
        User owner = findOwner(t.getId());
        var activeStores = physicalStoreRepository.findAllByTenantIdAndDeletedAtIsNullOrderByIsMainDescNameAsc(t.getId());
        return TenantMapper.toView(t, owner, stores, members, activeStores);
    }

    @Transactional
    public TenantView update(UUID id, UpdateTenantRequest req) {
        Tenant t = tenantRepository.findById(id)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        if (req.tradeName() != null) t.setTradeName(req.tradeName().trim());
        if (req.legalName() != null) t.setLegalName(req.legalName());
        if (req.cnpj() != null) t.setCnpj(CnpjValidator.format(req.cnpj()));
        if (req.description() != null) t.setDescription(req.description());
        if (req.phone() != null) t.setPhone(req.phone());
        if (req.whatsapp() != null) t.setWhatsapp(req.whatsapp());
        if (req.email() != null) t.setEmail(req.email());
        if (req.website() != null) t.setWebsite(req.website());
        if (req.logoUrl() != null) t.setLogoUrl(req.logoUrl());
        if (req.bannerUrl() != null) t.setBannerUrl(req.bannerUrl());
        if (req.status() != null) {
            try {
                t.setStatus(TenantStatus.valueOf(req.status()));
            } catch (IllegalArgumentException e) {
                throw TenantExceptions.badRequest("Status inválido: " + req.status());
            }
        }
        tenantRepository.save(t);
        return getById(id);
    }

    @Transactional
    public void softDelete(UUID id) {
        Tenant t = tenantRepository.findById(id)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        t.setDeletedAt(OffsetDateTime.now());
        t.setStatus(TenantStatus.canceled);
        tenantRepository.save(t);
        auditService.record("tenant.delete", "tenant", id.toString(), java.util.Map.of("slug", t.getSlug()));
    }

    private User findOwner(UUID tenantId) {
        return tenantUserRepository.findAllByTenantIdAndIsActiveTrue(tenantId).stream()
            .filter(tu -> tu.getRole() == TenantUserRole.OWNER)
            .findFirst()
            .flatMap(tu -> userRepository.findByIdAndDeletedAtIsNull(tu.getUserId()))
            .orElse(null);
    }

    private static java.util.Map<String, Object> defaultTheme() {
        var t = new HashMap<String, Object>();
        t.put("primary", "#667eea");
        t.put("secondary", "#11998e");
        t.put("accent", "#764ba2");
        t.put("dark", "#1a1a2e");
        t.put("darkPage", "#0f0f1a");
        return t;
    }
}
