package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.lgpd.ConsentService;
import br.com.rizermarketplaces.core.marketplace.lgpd.DataExportService;
import br.com.rizermarketplaces.core.marketplace.lgpd.DataRetentionJob;
import br.com.rizermarketplaces.core.marketplace.model.Consent;
import br.com.rizermarketplaces.core.marketplace.model.ConsentPurpose;
import br.com.rizermarketplaces.core.marketplace.model.DataExportRequest;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/me")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Me · LGPD", description = "Consentimentos, export e exclusão de conta")
public class LgpdController {

    private final ConsentService consentService;
    private final DataExportService dataExportService;
    private final DataRetentionJob dataRetentionJob;
    private final UserRepository userRepository;

    public LgpdController(
        ConsentService consentService,
        DataExportService dataExportService,
        DataRetentionJob dataRetentionJob,
        UserRepository userRepository
    ) {
        this.consentService = consentService;
        this.dataExportService = dataExportService;
        this.dataRetentionJob = dataRetentionJob;
        this.userRepository = userRepository;
    }

    @PostMapping("/consents")
    public Consent recordConsent(@RequestBody RecordConsentRequest body, HttpServletRequest request) {
        UUID userId = CurrentUser.require().getId();
        ConsentPurpose purpose;
        try { purpose = ConsentPurpose.valueOf(body.purpose()); }
        catch (IllegalArgumentException e) {
            throw TenantExceptions.badRequest("purpose inválido: " + body.purpose());
        }
        return consentService.record(
            userId, null, purpose, body.granted(), body.documentVersion(), request
        );
    }

    @GetMapping("/consents")
    public List<Consent> myConsents() {
        UUID userId = CurrentUser.require().getId();
        return consentService.latestForUser(userId);
    }

    @PostMapping("/data-export")
    public DataExportRequest requestExport() {
        UUID userId = CurrentUser.require().getId();
        return dataExportService.request(userId);
    }

    @GetMapping("/data-export")
    public List<DataExportRequest> myExports() {
        UUID userId = CurrentUser.require().getId();
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .map(u -> dataExportService.requestRepository().findAllByUserIdOrderByRequestedAtDesc(u.getId()))
            .orElse(List.of());
    }

    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestBody(required = false) DeleteAccountRequest body) {
        UUID userId = CurrentUser.require().getId();
        User u = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> TenantExceptions.notFound("Usuário"));
        // Anonimiza imediatamente
        dataRetentionJob.anonymizeUser(userId);
        // Marca soft-delete em 30 dias (em prod, isso seria um job; aqui só marcamos deletedAt agora
        // — na prática o user fica "inativo" com PII removida, e o Job de retenção pode então
        // marcar deletedAt após o período de carência).
        u.setDeletedAt(OffsetDateTime.now());
        userRepository.save(u);
        return ResponseEntity.ok(Map.of(
            "status", "anonymized",
            "message", "Seus dados pessoais foram anonimizados. O registro foi marcado para exclusão definitiva."
        ));
    }

    public record RecordConsentRequest(String purpose, boolean granted, String documentVersion) {}
    public record DeleteAccountRequest(String reason) {}
}
