package br.com.rizermarketplaces.core.marketplace.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public final class CurrentUser {

    private CurrentUser() {}

    public static AuthenticatedUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Autenticação necessária");
        }
        return u;
    }

    public static AuthenticatedUser orNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            return null;
        }
        return u;
    }
}
