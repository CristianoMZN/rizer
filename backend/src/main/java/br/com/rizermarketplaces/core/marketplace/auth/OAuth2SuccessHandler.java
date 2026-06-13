package br.com.rizermarketplaces.core.marketplace.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth2.success-redirect:http://localhost:3000/auth/callback}")
    private String successRedirect;

    public OAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken && authentication.getPrincipal() instanceof OAuth2User oauthUser) {
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");
            String picture = oauthUser.getAttribute("picture");
            String providerId = oauthUser.getAttribute("sub");
            AuthService.LoginResult result = authService.loginOrCreateFromOAuth(
                email, name, "google", providerId, picture
            );
            // Em produção, redirecionaríamos para o frontend passando tokens via cookie HttpOnly.
            // Aqui optamos por retornar JSON para o caso de API; o frontend pode tratar.
            getRedirectStrategy().sendRedirect(request, response,
                successRedirect + "?access=" + result.accessToken());
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
