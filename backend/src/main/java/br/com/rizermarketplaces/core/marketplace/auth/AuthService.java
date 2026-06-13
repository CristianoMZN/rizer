package br.com.rizermarketplaces.core.marketplace.auth;

import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.model.SystemRole;
import br.com.rizermarketplaces.core.marketplace.model.TenantUser;
import br.com.rizermarketplaces.core.marketplace.model.TenantUserRole;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.TenantUserRepository;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
        UserRepository userRepository,
        TenantUserRepository tenantUserRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResult loginWithPassword(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (user.getDeletedAt() != null || !user.isActive()) {
            throw new BadCredentialsException("Conta inativa");
        }
        if (user.getPasswordHash() == null) {
            throw new BadCredentialsException("Conta cadastrada via provedor externo. Use o login social.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        List<TenantMembership> memberships = loadMemberships(user.getId());
        return buildLoginResult(user, memberships, null);
    }

    @Transactional
    public LoginResult loginWithRefresh(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        Claims claims = jwtTokenProvider.parse(refreshToken);
        if (!"refresh".equals(claims.get("typ", String.class))) {
            throw new BadCredentialsException("Token não é de refresh");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Conta inativa");
        }
        List<TenantMembership> memberships = loadMemberships(user.getId());
        return buildLoginResult(user, memberships, null);
    }

    @Transactional
    public LoginResult loginOrCreateFromOAuth(String email, String name, String provider, String providerId, String avatarUrl) {
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> {
                Optional<User> byEmail = userRepository.findByEmail(email);
                if (byEmail.isPresent()) {
                    User u = byEmail.get();
                    u.setProvider(provider);
                    u.setProviderId(providerId);
                    if (u.getAvatarUrl() == null) u.setAvatarUrl(avatarUrl);
                    return userRepository.save(u);
                }
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name != null ? name : email);
                newUser.setProvider(provider);
                newUser.setProviderId(providerId);
                newUser.setAvatarUrl(avatarUrl);
                newUser.setSystemRole(SystemRole.user);
                return userRepository.save(newUser);
            });
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        List<TenantMembership> memberships = loadMemberships(user.getId());
        return buildLoginResult(user, memberships, null);
    }

    public LoginResult buildLoginResult(User user, List<TenantMembership> memberships, UUID selectedTenantId) {
        UUID tenantId = selectedTenantId;
        if (tenantId == null && !memberships.isEmpty()) {
            tenantId = memberships.get(0).tenantId();
        }
        String role = user.getSystemRole() == null ? "user" : user.getSystemRole().name();
        String access = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), role, tenantId);
        String refresh = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), role);
        UserView view = new UserView(
            user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl(),
            user.getSystemRole(), user.getPhone(), memberships, tenantId
        );
        return new LoginResult(access, refresh, jwtTokenProvider.getAccessTtlSeconds(), view);
    }

    public List<TenantMembership> loadMemberships(UUID userId) {
        return tenantUserRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
            .map(tu -> new TenantMembership(
                tu.getTenantId(),
                tu.getRole().name(),
                tu.getRole() == TenantUserRole.OWNER,
                tu.getRole() == TenantUserRole.MANAGER || tu.getRole() == TenantUserRole.OWNER
            ))
            .toList();
    }

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            throw new BadCredentialsException("Não autenticado");
        }
        return userRepository.findByIdAndDeletedAtIsNull(u.getId())
            .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
    }

    public Optional<UUID> currentTenantId() {
        return Optional.ofNullable(TenantContextHolder.getId());
    }

    public record LoginResult(String accessToken, String refreshToken, long expiresIn, UserView user) {}
    public record UserView(
        UUID id, String email, String name, String avatarUrl, SystemRole systemRole,
        String phone, List<TenantMembership> memberships, UUID currentTenantId
    ) {}
    public record TenantMembership(UUID tenantId, String role, boolean isOwner, boolean isManagerOrOwner) {}
}
