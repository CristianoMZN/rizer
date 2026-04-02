package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

// DTO (record) que representa os dados retornados para o endpoint /users/me.
// Java `record` é uma forma concisa de declarar uma classe imutável com campos, getters e construtor.
@Schema(name = "UserMeResponse", description = "Dados do usuario autenticado")
public record UserMeResponse(
    @Schema(description = "Identificador unico do usuario", example = "8f93d6dd-7fc0-491f-8e1e-c3f8e3f4f5cc")
    UUID id,
    @Schema(description = "Nome exibido no perfil", example = "Cristiano Oliveira")
    String name,
    @Schema(description = "Email principal da conta", example = "cristiano@exemplo.com")
    String email,
    @Schema(description = "URL da foto de perfil", example = "https://lh3.googleusercontent.com/a/default-user")
    String avatarUrl,
    @Schema(description = "Provedor de autenticacao", example = "google")
    String provider,
    @Schema(description = "Data de criacao no sistema")
    OffsetDateTime createdAt,
    @Schema(description = "Data da ultima atualizacao")
    OffsetDateTime updatedAt
) {

    // Método de fábrica para construir o DTO a partir da entidade User
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.getProvider(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
