package br.com.rizermarketplaces.core.marketplace.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfValidatorTest {

    @Test
    void validaCpfValido() {
        // CPFs gerados algoritmicamente
        assertTrue(CpfValidator.isValid("529.982.247-25"));
        assertTrue(CpfValidator.isValid("52998224725"));
        assertTrue(CpfValidator.isValid("111.444.777-35"));
        assertTrue(CpfValidator.isValid("11144477735"));
    }

    @Test
    void rejeitaCpfInvalido() {
        assertEquals(false, CpfValidator.isValid("529.982.247-00"));
        assertEquals(false, CpfValidator.isValid("111.444.777-00"));
        assertEquals(false, CpfValidator.isValid("00000000000"));
    }

    @Test
    void rejeitaCpfComTamanhoErrado() {
        assertEquals(false, CpfValidator.isValid("123"));
        assertEquals(false, CpfValidator.isValid("529982247251"));
    }

    @Test
    void rejeitaCpfNuloOuVazio() {
        assertEquals(false, CpfValidator.isValid(null));
        assertEquals(false, CpfValidator.isValid(""));
    }

    @Test
    void formataCpf() {
        assertEquals("529.982.247-25", CpfValidator.format("52998224725"));
        assertEquals("000.000.000-00", CpfValidator.format("00000000000"));
    }

    @Test
    void normalizaCpf() {
        assertEquals("52998224725", CpfValidator.normalize("529.982.247-25"));
        assertEquals("52998224725", CpfValidator.normalize("52998224725"));
        assertNull(CpfValidator.normalize(""));
        assertNull(CpfValidator.normalize(null));
    }
}
