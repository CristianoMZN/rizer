package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.auth.AuthService;
import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.auth.AuthenticatedUser;
import br.com.rizermarketplaces.core.marketplace.dto.LoginDTO;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
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
