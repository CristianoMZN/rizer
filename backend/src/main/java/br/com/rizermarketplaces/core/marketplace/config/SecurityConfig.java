package br.com.rizermarketplaces.core.marketplace.config;

import br.com.rizermarketplaces.core.marketplace.auth.JwtAuthenticationFilter;
import br.com.rizermarketplaces.core.marketplace.auth.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${GOOGLE_OAUTH_CLIENT_ID:}")
    private String googleClientId;

    @Value("${GOOGLE_OAUTH_CLIENT_SECRET:}")
    private String googleClientSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        OAuth2SuccessHandler oAuth2SuccessHandler
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Documentação + health
                .requestMatchers("/", "/docs", "/docs/**", "/openapi", "/openapi/**", "/swagger-ui.html", "/health", "/health/**").permitAll()
                // Auth + OAuth callbacks
                .requestMatchers("/auth/**", "/login/**", "/oauth2/**", "/error").permitAll()
                // Webhooks (assinados pelos provedores, validados no service)
                .requestMatchers("/billing/webhooks/**").permitAll()
                // Páginas legais (estáticas)
                .requestMatchers(HttpMethod.GET, "/legal/**").permitAll()
                // API pública por país
                .requestMatchers(HttpMethod.GET, "/*/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/tenants/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/countries/**").permitAll()
                // Demais exigem autenticação
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (isGoogleOAuthConfigured()) {
            http
                .oauth2Login(oauth -> oauth.successHandler(oAuth2SuccessHandler))
                .exceptionHandling(ex ->
                    ex.authenticationEntryPoint(
                        new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google")
                    )
                );
        } else {
            http
                .oauth2Login(oauth -> oauth.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        }

        return http.build();
    }

    private boolean isGoogleOAuthConfigured() {
        return googleClientId != null && !googleClientId.isBlank()
            && googleClientSecret != null && !googleClientSecret.isBlank();
    }

    @Bean
    @ConditionalOnExpression("'${GOOGLE_OAUTH_CLIENT_ID:}' != '' and '${GOOGLE_OAUTH_CLIENT_SECRET:}' != ''")
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration googleRegistration = ClientRegistrations.fromIssuerLocation("https://accounts.google.com")
            .registrationId("google")
            .clientId(googleClientId)
            .clientSecret(googleClientSecret)
            .scope("openid", "profile", "email")
            .build();

        return new InMemoryClientRegistrationRepository(googleRegistration);
    }

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    public OAuth2AuthorizedClientService authorizedClientService(
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    private org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        var config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOriginPatterns(java.util.List.of(
            "http://localhost:*", "http://127.0.0.1:*",
            "https://*.motorise.com.br", "https://motorise.com.br"
        ));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setExposedHeaders(java.util.List.of("Set-Cookie"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
