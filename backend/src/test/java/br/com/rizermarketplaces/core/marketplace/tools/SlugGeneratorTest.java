package br.com.rizermarketplaces.core.marketplace.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlugGeneratorTest {

    @Test
    void normalizaAcentos() {
        assertEquals("concessionaria", SlugGenerator.from("Concessionária"));
        assertEquals("rede-de-motos", SlugGenerator.from("Rede de Motos"));
        assertEquals("sao-paulo", SlugGenerator.from("São Paulo"));
    }

    @Test
    void removeCaracteresEspeciais() {
        assertEquals("abc123", SlugGenerator.from("ABC@!#$123"));
        assertEquals("ola-mundo", SlugGenerator.from("Olá, Mundo!"));
    }

    @Test
    void colapsaHifens() {
        assertEquals("a-b-c", SlugGenerator.from("a---b   c"));
        assertEquals("teste", SlugGenerator.from("---teste---"));
    }

    @Test
    void truncaEm80Caracteres() {
        String longInput = "a".repeat(200);
        String out = SlugGenerator.from(longInput);
        assertTrue(out.length() <= 80, "slug não pode passar de 80 chars: " + out.length());
    }

    @Test
    void trataInputsVazios() {
        assertEquals("", SlugGenerator.from(null));
        assertEquals("", SlugGenerator.from(""));
        assertEquals("", SlugGenerator.from("   "));
    }
}
