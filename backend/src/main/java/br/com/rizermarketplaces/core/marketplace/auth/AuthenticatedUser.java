package br.com.rizermarketplaces.core.marketplace.auth;

import br.com.rizermarketplaces.core.marketplace.model.SystemRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Representa o usuário autenticado no contexto do Spring Security.
 * O `tenantId` é mutável durante a sessão (o user pode trocar de tenant
 * sem re-logar), por isso não é parte do equals/hashCode.
 */
public class AuthenticatedUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String name;
    private final SystemRole systemRole;
    private final boolean active;
    private UUID currentTenantId;

    public AuthenticatedUser(UUID id, String email, String name, SystemRole systemRole, boolean active) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.systemRole = systemRole;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public SystemRole getSystemRole() { return systemRole; }
    public UUID getCurrentTenantId() { return currentTenantId; }
    public void setCurrentTenantId(UUID currentTenantId) { this.currentTenantId = currentTenantId; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + systemRole.name()));
    }

    @Override public String getPassword() { return ""; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return active; }
    @Override public boolean isAccountNonLocked() { return active; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
}
