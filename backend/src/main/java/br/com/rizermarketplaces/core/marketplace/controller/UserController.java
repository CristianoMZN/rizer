package br.com.rizermarketplaces.core.marketplace.controller;

/*
 * Controlador REST para operações relacionadas a usuários/autenticação.
 * Comentários explicam as anotações do Spring e o comportamento dos métodos.
 */

import br.com.rizermarketplaces.core.marketplace.dto.UserMeResponse;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController: indica que a classe é um controlador REST e que os
// retornos dos métodos são serializados diretamente no corpo da resposta (equivalente a @Controller + @ResponseBody).
@RestController
// @RequestMapping("/users"): define o caminho base para todos os endpoints deste controlador.
@RequestMapping("/users")
// @Tag: anotação usada pelo OpenAPI/Swagger para agrupar e documentar endpoints.
@Tag(name = "Usuarios", description = "Operacoes de conta e perfil do usuario autenticado")
public class UserController {

    // Injeção via construtor do serviço que contém a lógica de negócio relacionada a usuários.
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /users/me
     * Retorna os dados do usuário autenticado via OAuth2.
     * Se for o primeiro acesso, persiste o usuário no banco.
     */
    // @GetMapping: mapeia requisições HTTP GET para o caminho "/me".
    @GetMapping("/me")
    @Operation(
        // Metadata usada para documentação OpenAPI/Swagger
        summary = "Retorna o usuario autenticado",
        description = "Sincroniza os dados recebidos do OAuth2 no primeiro acesso e retorna o perfil atual."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Perfil retornado com sucesso",
            content = @Content(schema = @Schema(implementation = UserMeResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Usuario nao autenticado")
    })
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal OAuth2User principal) {
        // @AuthenticationPrincipal: injeta o principal (usuário autenticado) fornecido pelo Spring Security/OAuth2.
        // Aqui chamamos o serviço para sincronizar dados do OAuth2 e retornar um DTO apropriado para resposta.
        User user = userService.syncFromOAuth2(principal, "google");
        return ResponseEntity.ok(UserMeResponse.from(user));
    }
}
