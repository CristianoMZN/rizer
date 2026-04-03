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

import java.time.OffsetDateTime;

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
            content = @Content(schema = @Schema(implementation = HomeResponse.class))
        )
    })
    
    public HomeResponse home() {
        return new HomeResponse(
            "ok",
            "Bem-vindo ao Riser Marketplaces!",
            "/docs",
            OffsetDateTime.now()
        );
    }

    public record HomeResponse(
        @Schema(description = "Status do backend", example = "ok")
        String status,
        @Schema(description = "Mensagem de boas-vindas", example = "Bem-vindo ao Riser Marketplaces!")
        String message,
        @Schema(description = "URL da documentação da API", example = "/docs")
        String documentation,
        @Schema(description = "Data/hora atual do servidor", example = "2026-04-02T19:50:00Z")
        OffsetDateTime timestamp
    ) {
    }
}
