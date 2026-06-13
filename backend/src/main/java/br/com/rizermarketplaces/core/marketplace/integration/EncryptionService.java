package br.com.rizermarketplaces.core.marketplace.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia simétrica AES-GCM 256 para tokens sensíveis.
 *
 * Chave configurada via `app.encryption.key` (32 bytes em base64 = 44 chars).
 * Formato do ciphertext: base64( IV(12 bytes) || ciphertext || tag(16 bytes) )
 *
 * Em produção, troque para AWS KMS / Google Cloud KMS. Aqui a chave vive
 * em env, o que ainda é mais seguro que persistir em plaintext.
 */
@Service
public class EncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ALGO = "AES/GCM/NoPadding";

    @Value("${app.encryption.key:}")
    private String base64Key;

    private SecretKey getKey() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("app.encryption.key não configurada. Defina em backend/.env");
        }
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        if (bytes.length != 32) {
            throw new IllegalStateException("app.encryption.key deve decodificar para 32 bytes (AES-256)");
        }
        return new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar token", e);
        }
    }

    public String decrypt(String ciphertextB64) {
        if (ciphertextB64 == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(ciphertextB64);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ct = new byte[all.length - GCM_IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(all, GCM_IV_LENGTH, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao descriptografar token", e);
        }
    }
}
