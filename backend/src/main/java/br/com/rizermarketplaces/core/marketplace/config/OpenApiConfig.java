package br.com.rizermarketplaces.core.marketplace.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

// @Configuration: classe de configuração gerenciada pelo Spring (opcional aqui, serve apenas para registro).
@Configuration
// @OpenAPIDefinition: fornece metadados globais para a documentação OpenAPI/Swagger.
@OpenAPIDefinition(
    info = @Info(
        title = "Riser Marketplaces API",
        version = "v1",
        description = "API do servico de marketplace da Riser com contexto regional por prefixo de rota (ex: /BR, /US), suporte geoespacial com PostGIS e catalogo com atributos dinamicos por realm.",
        contact = @Contact(
            name = "Riser Team",
            email = "dev@riser.com"
        ),
        license = @License(
            name = "Proprietary"
        )
    )
)
public class OpenApiConfig {
    // Classe sem conteúdo: as anotações já configuram a documentação global.
}
