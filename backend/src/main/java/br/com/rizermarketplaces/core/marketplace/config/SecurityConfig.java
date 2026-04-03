package br.com.rizermarketplaces.core.marketplace.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

// @Configuration: indica que a classe define beans gerenciados pelo Spring.
// @EnableWebSecurity: habilita a configuração de segurança web do Spring Security.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${GOOGLE_OAUTH_CLIENT_ID:}")
    private String googleClientId;

    @Value("${GOOGLE_OAUTH_CLIENT_SECRET:}")
    private String googleClientSecret;

    // Define o bean SecurityFilterChain que configura o filtro de segurança da aplicação.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configura autorização de requisições
            .authorizeHttpRequests(auth -> auth
                // Permite acesso público à documentação e ao catálogo de produtos
                .requestMatchers("/", "/docs", "/docs/**", "/openapi", "/openapi/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/*/products", "/*/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/tenants/public/**").permitAll()
                // Qualquer outra requisição exige autenticação
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        if (isGoogleOAuthConfigured()) {
            http
                .oauth2Login(Customizer.withDefaults())
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
}
