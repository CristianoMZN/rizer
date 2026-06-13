package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "custom_domain_checks")
public class CustomDomainCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String domain;

    @Column(name = "expected_target", nullable = false, length = 255)
    private String expectedTarget;

    @Column(name = "cname_found", length = 255)
    private String cnameFound;

    @Column(name = "resolved_ip", length = 64)
    private String resolvedIp;

    @Column(nullable = false, length = 20)
    private String status;            // VERIFIED | FAILED | PENDING

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "checked_at", nullable = false)
    private OffsetDateTime checkedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getExpectedTarget() { return expectedTarget; }
    public void setExpectedTarget(String expectedTarget) { this.expectedTarget = expectedTarget; }
    public String getCnameFound() { return cnameFound; }
    public void setCnameFound(String cnameFound) { this.cnameFound = cnameFound; }
    public String getResolvedIp() { return resolvedIp; }
    public void setResolvedIp(String resolvedIp) { this.resolvedIp = resolvedIp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(OffsetDateTime checkedAt) { this.checkedAt = checkedAt; }
}
