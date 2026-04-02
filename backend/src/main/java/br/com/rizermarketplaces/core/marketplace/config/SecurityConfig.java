package br.com.rizermarketplaces.core.marketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

// @Configuration: indica que a classe define beans gerenciados pelo Spring.
// @EnableWebSecurity: habilita a configuração de segurança web do Spring Security.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Define o bean SecurityFilterChain que configura o filtro de segurança da aplicação.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configura autorização de requisições
            .authorizeHttpRequests(auth -> auth
                // Permite acesso público à documentação e ao catálogo de produtos
                .requestMatchers("/docs", "/docs/**", "/openapi", "/openapi/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/*/products", "/*/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/tenants/public/**").permitAll()
                // Qualquer outra requisição exige autenticação
                .anyRequest().authenticated()
            )
            // Habilita login via OAuth2 (redireciona para provedores configurados)
            .oauth2Login(Customizer.withDefaults())
            // Desabilita formulários e HTTP Basic já que OAuth2 é usado
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // Quando não autenticado, redireciona para o endpoint de autorização do Google
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google")
                )
            );

        return http.build();
    }
}
