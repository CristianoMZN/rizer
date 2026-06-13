package br.com.rizermarketplaces.core.marketplace.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhoneNormalizerTest {

    @Test
    void removeNaoDigitos() {
        assertEquals("11999998888", PhoneNormalizer.onlyDigits("(11) 99999-8888"));
        assertEquals("5511999998888", PhoneNormalizer.onlyDigits("+55 (11) 99999-8888"));
    }

    @Test
    void trataNuloOuVazio() {
        assertEquals("", PhoneNormalizer.onlyDigits(null));
        assertEquals("", PhoneNormalizer.onlyDigits(""));
        assertEquals("", PhoneNormalizer.onlyDigits("   "));
    }

    @Test
    void mantemApenasNumeros() {
        assertEquals("12345", PhoneNormalizer.onlyDigits("a1b2c3d4e5"));
    }
}
