package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.auth.AuthService;
import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.auth.AuthenticatedUser;
import br.com.rizermarketplaces.core.marketplace.dto.LoginDTO;
import br.com.rizermarketplaces.core.marketplace.dto.RegisterConsumerRequest;
import br.com.rizermarketplaces.core.marketplace.lgpd.ConsentService;
import br.com.rizermarketplaces.core.marketplace.model.ConsentPurpose;
import br.com.rizermarketplaces.core.marketplace.model.SystemRole;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import br.com.rizermarketplaces.core.marketplace.tools.CpfValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints de autenticação e autorização")
public class AuthController {

    private static final String ACCESS_COOKIE = "motorise_access";
    private static final String REFRESH_COOKIE = "motorise_refresh";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConsentService consentService;
    private final ObjectMapper objectMapper;

    public AuthController(
        AuthService authService,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        ConsentService consentService,
        ObjectMapper objectMapper
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.consentService = consentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/login")
    @Operation(summary = "Login com e-mail e senha")
    public ResponseEntity<AuthService.LoginResult> login(
        @RequestBody LoginDTO request,
        HttpServletResponse response
    ) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe e-mail e senha");
        }
        AuthService.LoginResult result = authService.loginWithPassword(request.getEmail(), request.getPassword());
        writeAuthCookies(response, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login/refresh")
    @Operation(summary = "Renovar access token usando refresh token")
    public ResponseEntity<AuthService.LoginResult> refresh(
        @CookieValue(name = REFRESH_COOKIE, required = false) String refreshCookie,
        @RequestHeader(name = "X-Refresh-Token", required = false) String refreshHeader,
        HttpServletResponse response
    ) {
        String token = refreshHeader != null ? refreshHeader : refreshCookie;
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token ausente");
        }
        try {
            AuthService.LoginResult result = authService.loginWithRefresh(token);
            writeAuthCookies(response, result.accessToken(), result.refreshToken());
            return ResponseEntity.ok(result);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registro público de consumidor final")
    public ResponseEntity<AuthService.LoginResult> register(
        @RequestBody RegisterConsumerRequest body,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        if (!body.getPassword().equals(body.getPasswordConfirmation())) {
            throw TenantExceptions.badRequest("Confirmação de senha não confere");
        }
        String email = body.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(u -> {
            throw TenantExceptions.conflict("E-mail já cadastrado");
        });
        String cpfNormalized = null;
        if (body.getCpf() != null && !body.getCpf().isBlank()) {
            if (!CpfValidator.isValid(body.getCpf())) {
                throw TenantExceptions.badRequest("CPF inválido");
            }
            cpfNormalized = CpfValidator.format(body.getCpf());
            userRepository.findByCpfAndDeletedAtIsNull(cpfNormalized).ifPresent(u -> {
                throw TenantExceptions.conflict("CPF já cadastrado");
            });
        }

        User u = new User();
        u.setEmail(email);
        u.setName(body.getName().trim());
        u.setPhone(body.getPhone().trim());
        u.setCpf(cpfNormalized);
        u.setBirthDate(body.getBirthDate());
        u.setAvatarUrl(body.getAvatarUrl());
        u.setProvider("local");
        u.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        u.setSystemRole(SystemRole.user);
        u.setProfileCompleted(cpfNormalized != null);
        u = userRepository.save(u);

        consentService.record(u.getId(), null, ConsentPurpose.terms_of_use, true, body.getTermsVersion(), request);
        consentService.record(u.getId(), null, ConsentPurpose.privacy_policy, true, body.getPrivacyVersion(), request);

        AuthService.LoginResult result = authService.buildLoginResult(u, java.util.List.of(), null);
        writeAuthCookies(response, result.accessToken(), result.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/facebook")
    @Operation(summary = "Login com Facebook (accessToken do FB SDK)")
    public ResponseEntity<AuthService.LoginResult> loginWithFacebook(
        @RequestBody Map<String, String> body,
        HttpServletResponse response
    ) {
        String accessToken = body.get("accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            throw TenantExceptions.badRequest("accessToken é obrigatório");
        }
        try {
            String url = "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token="
                + accessToken;
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new BadCredentialsException("Token Facebook inválido");
            }
            JsonNode payload = objectMapper.readTree(conn.getInputStream());
            String fbId = payload.path("id").asText(null);
            String email = payload.path("email").asText(null);
            String name = payload.path("name").asText(null);
            String picture = payload.path("picture").path("data").path("url").asText(null);
            if (fbId == null || email == null) {
                throw new BadCredentialsException("Resposta do Facebook sem id/email");
            }
            if (email == null || email.isBlank()) {
                throw TenantExceptions.badRequest("Conta Facebook sem e-mail. Conecte um e-mail à sua conta.");
            }
            AuthService.LoginResult result = authService.loginOrCreateFromOAuth(
                email, name != null ? name : email, "facebook", fbId, picture
            );
            writeAuthCookies(response, result.accessToken(), result.refreshToken());
            return ResponseEntity.ok(result);
        } catch (BadCredentialsException e) {
            throw TenantExceptions.badRequest(e.getMessage());
        } catch (Exception e) {
            throw TenantExceptions.badRequest("Falha ao validar token do Facebook: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (limpa cookies)")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        clearCookie(response, ACCESS_COOKIE);
        clearCookie(response, REFRESH_COOKIE);
        return ResponseEntity.ok(Map.of("message", "Logout realizado"));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário autenticado + memberships")
    public ResponseEntity<AuthService.UserView> me() {
        AuthenticatedUser principal = CurrentUser.require();
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        return ResponseEntity.ok(new AuthService.UserView(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getAvatarUrl(),
            user.getSystemRole(),
            user.getPhone(),
            authService.loadMemberships(user.getId()),
            principal.getCurrentTenantId()
        ));
    }

    @PostMapping("/switch-tenant")
    @Operation(summary = "Selecionar tenant ativo (devolve novo access token com claim tenantId)")
    public ResponseEntity<AuthService.LoginResult> switchTenant(@RequestBody Map<String, String> body) {
        String tenantId = body.get("tenantId");
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId é obrigatório");
        }
        AuthenticatedUser principal = CurrentUser.require();
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        var memberships = authService.loadMemberships(user.getId());
        boolean allowed = memberships.stream().anyMatch(m -> m.tenantId().equals(UUID.fromString(tenantId)));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem acesso a este tenant");
        }
        AuthService.LoginResult result = authService.buildLoginResult(user, memberships, UUID.fromString(tenantId));
        return ResponseEntity.ok(result);
    }

    private void writeAuthCookies(HttpServletResponse response, String access, String refresh) {
        response.addHeader("Set-Cookie", buildCookie(ACCESS_COOKIE, access, 60 * 60));
        response.addHeader("Set-Cookie", buildCookie(REFRESH_COOKIE, refresh, 60 * 60 * 24 * 30));
    }

    private String buildCookie(String name, String value, int maxAge) {
        return String.format(
            "%s=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
            name, value, maxAge
        );
    }

    private void clearCookie(HttpServletResponse response, String name) {
        response.addHeader("Set-Cookie", name + "=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax");
    }
}
