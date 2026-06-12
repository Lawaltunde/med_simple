package com.medsimple.shared.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EncryptionServiceTest {

    // 32 zero bytes — valid AES-256 key for tests only.
    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(TEST_KEY);
    }

    @Test
    void encryptAndDecrypt_roundTrip() {
        String plaintext = "AB123456789";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() {
        String plaintext = "AB123456789";
        String first = encryptionService.encrypt(plaintext);
        String second = encryptionService.encrypt(plaintext);
        // Random IV guarantees different ciphertext per call
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void encrypt_ciphertextDoesNotContainPlaintext() {
        String plaintext = "OHIP1234567890AB";
        String encrypted = encryptionService.encrypt(plaintext);
        assertThat(encrypted).doesNotContain(plaintext);
    }

    @Test
    void decrypt_tamperDetection() {
        String encrypted = encryptionService.encrypt("secret");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "XXXX";
        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    void constructor_rejectsShortKey() {
        // 16-byte key is AES-128, not AES-256 — must be rejected
        String shortKey = "AAAAAAAAAAAAAAAAAAAAAA=="; // 16 bytes
        assertThatThrownBy(() -> new EncryptionService(shortKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32-byte");
    }
}
