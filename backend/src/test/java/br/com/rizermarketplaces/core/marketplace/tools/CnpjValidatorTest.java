package br.com.rizermarketplaces.core.marketplace.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CnpjValidatorTest {

    @Test
    void validaCnpjValido() {
        // CNPJ válido: 11.444.777/0001-61 (DV verificado algoritmicamente)
        assertTrue(CnpjValidator.isValid("11.444.777/0001-61"));
        assertTrue(CnpjValidator.isValid("11444777000161"));
    }

    @Test
    void rejeitaCnpjInvalido() {
        assertEquals(false, CnpjValidator.isValid("11.444.777/0001-00"));
        assertEquals(false, CnpjValidator.isValid("00000000000000")); // todos zeros
    }

    @Test
    void rejeitaCnpjComTamanhoErrado() {
        assertEquals(false, CnpjValidator.isValid("123"));
        assertEquals(false, CnpjValidator.isValid("123456789012345"));
    }

    @Test
    void rejeitaCnpjNuloOuVazio() {
        assertEquals(false, CnpjValidator.isValid(null));
        assertEquals(false, CnpjValidator.isValid(""));
    }

    @Test
    void formataCnpj() {
        assertEquals("11.444.777/0001-61", CnpjValidator.format("11444777000161"));
        assertEquals("00.000.000/0000-00", CnpjValidator.format("00000000000000"));
    }
}
