package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.dto.LoginDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;



@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints de autenticação e autorização")
public class AuthController {
    @PostMapping("/login/token")
    @Operation(summary = "Login JWT", description = "Endpoint para autenticação de usuários. Recebe credenciais e retorna tokens de acesso e atualização.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Login bem-sucedido",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        )
    })
    public String loginToken() {
        return "API está online!";
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Endpoint para autenticação de usuários. Recebe credenciais e retorna tokens de acesso e atualização.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Login bem-sucedido",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        )
    })
    public ResponseEntity<?> login(@RequestBody LoginDTO loginRequest, HttpServletResponse response) {
        var maybeEmail = loginRequest.getEmail();
        var maybeUsername = loginRequest.getUsername();
        var password = loginRequest.getPassword();
        if(maybeEmail == null && maybeUsername == null) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Email ou username devem ser fornecidos.")).build();
        }
        if(password == null) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Senha deve ser fornecida.")).build();
        }
        
        Cookie cookie = new Cookie("access_token", "TOKEN");

        cookie.setHttpOnly(true);   // Impede acesso via JavaScript
        cookie.setSecure(true);     // Garante que só viaja em HTTPS (use false em localhost)
        cookie.setPath("/");        // Disponível em toda a aplicação
        cookie.setMaxAge(86400);    // Expiração (ex: 24 horas)
        // cookie.setAttribute("SameSite", "Strict"); // Proteção contra CSRF

        // 4. Adicionar ao response
        response.addCookie(cookie);
        return ResponseEntity.ok("Login realizado com sucesso e cookie definido!");
    }



    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Endpoint para renovação do token de acesso usando um token de atualização válido.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token renovado com sucesso",
            content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))
        )
    })
    public String refreshToken() {
        return "API está online!";
    }


    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Endpoint para logout de usuários. Invalida o token de acesso.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logout bem-sucedido",
            content = @Content(schema = @Schema(implementation = LogoutResponse.class))
        )
    })
    public String logout() {
        return "API está online!";
    }


    public record AuthResponse(
        @Schema(description = "Token de Acesso", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String access_token,
        @Schema(description = "Tipo de Token", example = "Bearer")
        String token_type,
        @Schema(description = "Tempo de expiração do token", example = "3600")
        String expires_in,
        @Schema(description = "Token de atualização", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refresh_token,
        @Schema(description = "Dados do Usuário", example = "{}")
        String user_data
    ) {
    }


    public record RefreshTokenResponse(
        @Schema(description = "Novo Token de Acesso", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String access_token,
        @Schema(description = "Tipo de Token", example = "Bearer")
        String token_type,
        @Schema(description = "Tempo de expiração do token", example = "3600")
        String expires_in,
        @Schema(description = "Novo Token de atualização", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refresh_token
    ) {
    }
    public record LogoutResponse(
        @Schema(description = "Mensagem de logout", example = "Logout bem-sucedido")
        String message
    ) {
    }
}
