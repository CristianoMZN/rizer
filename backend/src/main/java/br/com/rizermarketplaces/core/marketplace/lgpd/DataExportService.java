package br.com.rizermarketplaces.core.marketplace.lgpd;

import br.com.rizermarketplaces.core.marketplace.model.DataExportRequest;
import br.com.rizermarketplaces.core.marketplace.model.DataExportStatus;
import br.com.rizermarketplaces.core.marketplace.repository.AddressRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ConsentRepository;
import br.com.rizermarketplaces.core.marketplace.repository.DataExportRequestRepository;
import br.com.rizermarketplaces.core.marketplace.repository.PaymentRepository;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantUserRepository;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LGPD art. 18, V: direito de obter cópia dos dados.
 * Gera um JSON com tudo do user, sobe para S3 (bucket privado),
 * cria presigned URL válida por 7 dias, e atualiza o status da request.
 */
@Service
public class DataExportService {

    private static final Logger log = LoggerFactory.getLogger(DataExportService.class);
    private static final long URL_TTL_DAYS = 7;

    private final DataExportRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;
    private final AddressRepository addressRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final PhysicalStoreRepository physicalStoreRepository;
    private final S3StorageService s3;
    private final ObjectMapper mapper = new ObjectMapper();

    public DataExportService(
        DataExportRequestRepository requestRepository,
        UserRepository userRepository,
        ConsentRepository consentRepository,
        AddressRepository addressRepository,
        TenantUserRepository tenantUserRepository,
        PaymentRepository paymentRepository,
        ProductRepository productRepository,
        PhysicalStoreRepository physicalStoreRepository,
        S3StorageService s3
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.consentRepository = consentRepository;
        this.addressRepository = addressRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.physicalStoreRepository = physicalStoreRepository;
        this.s3 = s3;
    }

    @Transactional
    public DataExportRequest request(UUID userId) {
        if (userId == null) throw new IllegalArgumentException("userId obrigatório");
        DataExportRequest req = new DataExportRequest();
        req.setUserId(userId);
        req.setStatus(DataExportStatus.pending);
        req = requestRepository.save(req);
        processAsync(req.getId(), userId);
        return req;
    }

    @Transactional(readOnly = true)
    public List<DataExportRequest> listRequests(UUID userId) {
        return requestRepository.findAllByUserIdOrderByRequestedAtDesc(userId);
    }

    @Async
    @Transactional
    public void processAsync(UUID requestId, UUID userId) {
        process(requestId, userId);
    }

    @Transactional
    public void process(UUID requestId, UUID userId) {
        DataExportRequest req = requestRepository.findById(requestId).orElseThrow();
        try {
            req.setStatus(DataExportStatus.processing);
            requestRepository.save(req);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("generatedAt", OffsetDateTime.now().toString());
            payload.put("user", userRepository.findByIdAndDeletedAtIsNull(userId).map(this::userToMap).orElse(null));
            payload.put("addresses", addressRepository.findByUserIdAndDeletedAtIsNull(userId));
            payload.put("consents", consentRepository.findByUserAndPurpose(userId, br.com.rizermarketplaces.core.marketplace.model.ConsentPurpose.terms_of_use));
            payload.put("tenantMemberships", tenantUserRepository.findAllByUserIdAndIsActiveTrue(userId).stream().map(tu -> {
                var m = new LinkedHashMap<String, Object>();
                m.put("tenantId", tu.getTenantId());
                m.put("role", tu.getRole());
                m.put("physicalStoreIds", tu.getPhysicalStoreIds());
                m.put("isActive", tu.isActive());
                m.put("acceptedAt", tu.getAcceptedAt());
                return m;
            }).toList());
            payload.put("payments", paymentRepository.findAllByTenantIdOrderByPaidAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, 1000)).getContent());
            payload.put("products", productRepository.findAll().stream().filter(p -> userId.equals(p.getCreatedByUserId())).toList());
            payload.put("physicalStoresOwned", physicalStoreRepository.findAll().stream()
                .filter(s -> userId.equals(s.getCreatedByUserId())).toList());

            byte[] bytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
            String key = "data-export/" + userId + "/" + requestId + ".json";
            // upload manual via S3 (S3StorageService não tem helper de bytes puros, mas expõe o client)
            s3.uploadBytes(bytes, key, "application/json", "users/" + userId);

            var presigned = s3.getPresignedUrl(key);
            req.setStorageKey(key);
            req.setDownloadUrl(presigned.presignedUrl());
            req.setUrlExpiresAt(OffsetDateTime.ofInstant(presigned.expiresAt(), java.time.ZoneOffset.UTC));
            req.setStatus(DataExportStatus.ready);
            req.setCompletedAt(OffsetDateTime.now());
            requestRepository.save(req);
            log.info("[data-export] user={} request={} status=ready url_expires={}",
                userId, requestId, presigned.expiresAt());
        } catch (Exception e) {
            log.error("[data-export] user={} request={} falhou: {}", userId, requestId, e.getMessage(), e);
            req.setStatus(DataExportStatus.failed);
            req.setErrorMessage(e.getMessage());
            requestRepository.save(req);
        }
    }

    private Map<String, Object> userToMap(br.com.rizermarketplaces.core.marketplace.model.User u) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("name", u.getName());
        m.put("phone", u.getPhone());
        m.put("avatarUrl", u.getAvatarUrl());
        m.put("systemRole", u.getSystemRole());
        m.put("provider", u.getProvider());
        m.put("createdAt", u.getCreatedAt());
        return m;
    }
}
