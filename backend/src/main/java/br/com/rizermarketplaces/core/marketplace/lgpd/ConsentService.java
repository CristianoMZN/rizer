package br.com.rizermarketplaces.core.marketplace.lgpd;

import br.com.rizermarketplaces.core.marketplace.model.Consent;
import br.com.rizermarketplaces.core.marketplace.model.ConsentPurpose;
import br.com.rizermarketplaces.core.marketplace.repository.ConsentRepository;
import br.com.rizermarketplaces.core.marketplace.tools.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * LGPD: registra consentimentos do titular dos dados.
 *
 * - user logado: passa userId
 * - visitante: passa anonymousId (cookie uuid gerado pelo frontend)
 *
 * Cada consent guarda a `document_version` do termo na hora do aceite.
 * Quando o termo muda de versão, o frontend pede re-consent.
 */
@Service
public class ConsentService {

    private final ConsentRepository consentRepository;

    public ConsentService(ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    @Transactional
    public Consent record(
        UUID userId, String anonymousId, ConsentPurpose purpose,
        boolean granted, String documentVersion, HttpServletRequest request
    ) {
        if (userId == null && (anonymousId == null || anonymousId.isBlank())) {
            throw new IllegalArgumentException("userId ou anonymousId é obrigatório");
        }
        if (documentVersion == null || documentVersion.isBlank()) {
            throw new IllegalArgumentException("documentVersion é obrigatório");
        }
        Consent c = new Consent();
        c.setUserId(userId);
        c.setAnonymousId(anonymousId);
        c.setPurpose(purpose);
        c.setGranted(granted);
        c.setDocumentVersion(documentVersion);
        if (request != null) {
            c.setIp(RequestUtils.extractIp(request));
            c.setUserAgent(request.getHeader("User-Agent"));
        }
        return consentRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<Consent> latestForUser(UUID userId) {
        return consentRepository.findByUserAndPurpose(userId, ConsentPurpose.terms_of_use);
    }
}
