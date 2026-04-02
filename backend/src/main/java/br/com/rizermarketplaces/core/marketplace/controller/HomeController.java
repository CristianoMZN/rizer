package br.com.rizermarketplaces.core.marketplace.controller;

/*
 * Controlador simples para endpoints públicos/health-check da API.
 * Explica anotações de documentação e mapeamento de rota.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Sistema", description = "Endpoints de status e informacoes gerais da API")
public class HomeController {

    // @GetMapping("/"): mapeia requisições GET para a raiz '/' da aplicação.
    @GetMapping("/")
    @Operation(summary = "Mensagem de boas-vindas", description = "Endpoint simples para validar se a API esta online.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API online",
            // content com exemplo simples para documentação Swagger/OpenAPI
            content = @Content(schema = @Schema(example = "Bem-vindo ao Riser Marketplaces!"))
        )
    })
    public String home() {
        // Retorna uma mensagem simples; frameworks e clientes usam esse endpoint para health-checks.
        return "Bem-vindo ao Riser Marketplaces!";
    }   
}
