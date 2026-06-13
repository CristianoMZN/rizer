package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.InviteMemberRequest;
import br.com.rizermarketplaces.core.marketplace.dto.MemberView;
import br.com.rizermarketplaces.core.marketplace.model.PhysicalStore;
import br.com.rizermarketplaces.core.marketplace.model.SystemRole;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.TenantUser;
import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantUserRepository;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TenantMemberService {

    private final TenantUserRepository tenantUserRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PhysicalStoreRepository physicalStoreRepository;

    public TenantMemberService(
        TenantUserRepository tenantUserRepository,
        UserRepository userRepository,
        TenantRepository tenantRepository,
        PhysicalStoreRepository physicalStoreRepository
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.physicalStoreRepository = physicalStoreRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberView> list(UUID tenantId) {
        ensureTenant(tenantId);
        return tenantUserRepository.findAllByTenantIdAndIsActiveTrue(tenantId).stream()
            .map(this::toView)
            .toList();
    }

    @Transactional
    public MemberView invite(InviteMemberRequest req, UUID actorId) {
        Tenant tenant = ensureTenant(req.tenantId());
        validateStoreIds(tenant.getId(), req.physicalStoreIds());

        String email = req.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(req.name().trim());
            u.setProvider("local");
            u.setSystemRole(toSystemRole(req.role()));
            return userRepository.save(u);
        });

        if (user.getSystemRole() == SystemRole.user) {
            user.setSystemRole(toSystemRole(req.role()));
            userRepository.save(user);
        }

        TenantUser link = tenantUserRepository.findByTenantIdAndUserIdAndIsActiveTrue(tenant.getId(), user.getId())
            .orElseGet(TenantUser::new);
        link.setTenantId(tenant.getId());
        link.setUserId(user.getId());
        link.setRole(req.role());
        link.setActive(true);
        link.setInvitedByUserId(actorId);
        if (link.getInvitedAt() == null) link.setInvitedAt(OffsetDateTime.now());
        link.setAcceptedAt(OffsetDateTime.now());
        link.setPhysicalStoreIds(req.physicalStoreIds() == null ? new UUID[0] :
            req.physicalStoreIds().toArray(new UUID[0]));
        link = tenantUserRepository.save(link);

        return toView(link);
    }

    @Transactional
    public MemberView updateRole(UUID tenantId, UUID memberId, TenantUserRole role, List<UUID> storeIds) {
        TenantUser tu = tenantUserRepository.findById(memberId)
            .orElseThrow(() -> TenantExceptions.notFound("Membro"));
        if (!tu.getTenantId().equals(tenantId)) throw TenantExceptions.notFound("Membro");
        validateStoreIds(tenantId, storeIds);
        tu.setRole(role);
        if (storeIds != null) {
            tu.setPhysicalStoreIds(storeIds.toArray(new UUID[0]));
        }
        return toView(tenantUserRepository.save(tu));
    }

    @Transactional
    public void remove(UUID tenantId, UUID memberId) {
        TenantUser tu = tenantUserRepository.findById(memberId)
            .orElseThrow(() -> TenantExceptions.notFound("Membro"));
        if (!tu.getTenantId().equals(tenantId)) throw TenantExceptions.notFound("Membro");
        tu.setActive(false);
        tenantUserRepository.save(tu);
    }

    private Tenant ensureTenant(UUID tenantId) {
        Tenant t = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        if (t.getDeletedAt() != null) throw TenantExceptions.notFound("Tenant");
        return t;
    }

    private void validateStoreIds(UUID tenantId, List<UUID> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) return;
        var all = physicalStoreRepository.findAllByTenantIdAndDeletedAtIsNullOrderByIsMainDescNameAsc(tenantId)
            .stream().map(PhysicalStore::getId).toList();
        for (UUID id : storeIds) {
            if (!all.contains(id)) {
                throw TenantExceptions.badRequest("Loja inválida: " + id);
            }
        }
    }

    private MemberView toView(TenantUser tu) {
        User user = userRepository.findByIdAndDeletedAtIsNull(tu.getUserId()).orElse(null);
        Tenant tenant = tenantRepository.findById(tu.getTenantId()).orElse(null);
        List<UUID> storeIds = tu.getPhysicalStoreIds() == null ? List.of() :
            new ArrayList<>(List.of(tu.getPhysicalStoreIds()));
        return new MemberView(
            tu.getId(), tu.getTenantId(),
            tenant != null ? tenant.getSlug() : null,
            tenant != null ? tenant.getTradeName() : null,
            tu.getUserId(),
            user != null ? user.getName() : null,
            user != null ? user.getEmail() : null,
            tu.getRole(),
            storeIds,
            tu.isActive(),
            tu.getAcceptedAt(),
            tu.getExpireAt()
        );
    }

    private SystemRole toSystemRole(TenantUserRole role) {
        return switch (role) {
            case OWNER -> SystemRole.agency_owner;
            case MANAGER -> SystemRole.agency_admin;
            case SELLER -> SystemRole.agency_employee;
        };
    }
}
