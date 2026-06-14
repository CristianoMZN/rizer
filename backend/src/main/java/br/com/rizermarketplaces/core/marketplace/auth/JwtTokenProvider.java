package br.com.rizermarketplaces.core.marketplace.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Geração e validação de tokens JWT (HS256).
 * Claims:
 *  - sub: userId (UUID)
 *  - email: e-mail do usuário
 *  - role: system_role
 *  - tenantId: tenant atual selecionado (opcional, presente em chamadas autenticadas)
 *  - typ: "access" | "refresh"
 */
@Service
public class JwtTokenProvider {

    @Value("${app.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.access-ttl-minutes:60}")
    private long accessTtlMinutes;

    @Value("${app.jwt.refresh-ttl-days:30}")
    private long refreshTtlDays;

    @Value("${app.jwt.issuer:motorise}")
    private String issuer;

    private SecretKey getKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret não configurado. Defina em backend/.env");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret deve ter ao menos 32 bytes (256 bits) para HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(UUID userId, String email, String role, UUID tenantId) {
        return generate(userId, email, role, tenantId, "access", accessTtlMinutes * 60L);
    }

    public String generateRefreshToken(UUID userId, String email, String role) {
        return generate(userId, email, role, null, "refresh", refreshTtlDays * 86400L);
    }

    private String generate(UUID userId, String email, String role, UUID tenantId, String typ, long ttlSeconds) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
            .subject(userId.toString())
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttlSeconds)))
            .claims(Map.of(
                "email", email,
                "role", role,
                "typ", typ
            ));
        if (tenantId != null) {
            builder.claim("tenantId", tenantId.toString());
        }
        return builder.signWith(getKey()).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTtlSeconds() {
        return accessTtlMinutes * 60L;
    }

}
