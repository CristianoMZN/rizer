package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.SystemRole;
import br.com.rizermarketplaces.core.marketplace.model.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ConsumerProfileView {

    private final UUID id;
    private final String email;
    private final String name;
    private final String phone;
    private final String cpf;
    private final LocalDate birthDate;
    private final String avatarUrl;
    private final SystemRole systemRole;
    private final boolean profileCompleted;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public ConsumerProfileView(
        UUID id, String email, String name, String phone, String cpf, LocalDate birthDate,
        String avatarUrl, SystemRole systemRole, boolean profileCompleted,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.cpf = cpf;
        this.birthDate = birthDate;
        this.avatarUrl = avatarUrl;
        this.systemRole = systemRole;
        this.profileCompleted = profileCompleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ConsumerProfileView from(User u) {
        return new ConsumerProfileView(
            u.getId(), u.getEmail(), u.getName(), u.getPhone(), u.getCpf(), u.getBirthDate(),
            u.getAvatarUrl(), u.getSystemRole(), u.isProfileCompleted(),
            u.getCreatedAt(), u.getUpdatedAt()
        );
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getCpf() { return cpf; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getAvatarUrl() { return avatarUrl; }
    public SystemRole getSystemRole() { return systemRole; }
    public boolean isProfileCompleted() { return profileCompleted; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
