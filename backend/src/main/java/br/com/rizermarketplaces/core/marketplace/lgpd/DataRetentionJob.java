package br.com.rizermarketplaces.core.marketplace.lgpd;

import br.com.rizermarketplaces.core.marketplace.model.DataExportStatus;
import br.com.rizermarketplaces.core.marketplace.model.Payment;
import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.DataExportRequestRepository;
import br.com.rizermarketplaces.core.marketplace.repository.PaymentRepository;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * LGPD: retenção + minimização de dados.
 *
 * - DataExportRequest com URL expirada (>7 dias) → marca como EXPIRED
 *   e remove o arquivo do S3.
 * - Payments: mantidos por 5 anos (prazo fiscal/legal).
 * - Audit log: 5 anos.
 *
 * Leads/leads_old_data sem interação: anonimizar (em fase futura quando
 * houver a tabela leads — V15+).
 */
@Component
public class DataRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionJob.class);
    private static final int EXPORT_TTL_DAYS = 7;

    private final DataExportRequestRepository requestRepository;
    private final S3StorageService s3;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Value("${app.lgpd.retention-job-enabled:true}")
    private boolean enabled;

    public DataRetentionJob(
        DataExportRequestRepository requestRepository,
        S3StorageService s3,
        PaymentRepository paymentRepository,
        UserRepository userRepository
    ) {
        this.requestRepository = requestRepository;
        this.s3 = s3;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedDelayString = "${app.lgpd.retention-job-interval-ms:86400000}", initialDelay = 60_000)
    @Transactional
    public void run() {
        if (!enabled) return;
        OffsetDateTime now = OffsetDateTime.now();
        int expired = 0;
        // 1) Marca exports expirados e remove arquivos do S3
        for (var req : requestRepository.findAll()) {
            if (req.getStatus() == DataExportStatus.ready
                && req.getUrlExpiresAt() != null
                && req.getUrlExpiresAt().isBefore(now)) {
                req.setStatus(DataExportStatus.expired);
                if (req.getStorageKey() != null) {
                    try { s3.deleteFile(req.getStorageKey(), null); }
                    catch (Exception e) { log.warn("[retention] delete s3 falhou: {}", e.getMessage()); }
                }
                requestRepository.save(req);
                expired++;
            }
        }
        if (expired > 0) {
            log.info("[retention] exports expirados: {}", expired);
        }
    }

    /**
     * Anonimiza o user: substitui PII por placeholders, mantém o registro
     * (necessário para integridade de FKs: pagamentos, leads, etc.) e
     * apaga tokens de integração e o avatar.
     * Em conformidade com o art. 16 LGPD (eliminação de dados desnecessários).
     *
     * Após 30 dias o user é soft-deleted definitivamente (User.deletedAt set).
     */
    @Transactional
    public void anonymizeUser(UUID userId) {
        User u = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        u.setEmail("anon+" + userId + "@deleted.local");
        u.setName("[Conta excluída]");
        u.setPhone(null);
        u.setPasswordHash(null);
        u.setAvatarUrl(null);
        u.setProvider("deleted");
        u.setProviderId(null);
        u.setActive(false);
        u.setAttributes(new java.util.HashMap<>());
        userRepository.save(u);
    }
}
