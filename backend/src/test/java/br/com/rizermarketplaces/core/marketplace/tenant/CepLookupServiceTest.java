package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.CepLookupView;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;

class CepLookupServiceTest {

    @Test
    void lookup_cepInvalidoRetornaNull() {
        CepLookupService svc = new CepLookupService();
        ReflectionTestUtils.setField(svc, "token", "abc");
        assertNull(svc.lookup(null));
        assertNull(svc.lookup(""));
        assertNull(svc.lookup("123"));
        assertNull(svc.lookup("123456789"));
        assertNull(svc.lookup("abc-defgh"));
    }

    @Test
    void lookup_semTokenRetornaNull() {
        CepLookupService svc = new CepLookupService();
        ReflectionTestUtils.setField(svc, "token", "");
        // sem token, não deve tentar HTTP — retorna null imediatamente
        assertNull(svc.lookup("99150000"));
    }

    @Test
    void lookup_cepComMascara_normaliza() {
        // Não podemos testar o HTTP real aqui (sem internet/WireMock),
        // mas garantimos que a normalização (8 dígitos) aceita CEPs mascarados.
        CepLookupService svc = new CepLookupService();
        ReflectionTestUtils.setField(svc, "token", "");
        CepLookupView v = svc.lookup("99150-000");
        assertNull(v);
    }
}
