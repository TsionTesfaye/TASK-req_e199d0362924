package com.rescuehub;

import com.rescuehub.service.CryptoService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CryptoService.
 *
 * The integration tests cover the happy-path (valid 32-byte key) via the
 * PatientService tests.  These tests cover the remaining branches:
 *  - constructor rejects null/blank key
 *  - constructor rejects key shorter than 32 bytes
 *  - encrypt/decrypt round-trip
 *  - sha256Hex produces a stable, well-formed digest
 */
class CryptoServiceTest {

    private CryptoService service(String key) {
        return new CryptoService(key);
    }

    @Test
    void constructor_nullKey_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> service(null));
    }

    @Test
    void constructor_blankKey_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> service("   "));
    }

    @Test
    void constructor_emptyKey_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> service(""));
    }

    @Test
    void constructor_keyTooShort_throwsIllegalState() {
        // 31 bytes — one byte short of the 32-byte minimum
        assertThrows(IllegalStateException.class, () -> service("this-key-is-only-31-bytes-long!"));
    }

    @Test
    void constructor_exactly32Bytes_doesNotThrow() {
        assertDoesNotThrow(() -> service("0123456789abcdef0123456789abcdef"));
    }

    @Test
    void encryptDecrypt_roundTrip() {
        CryptoService svc = service("0123456789abcdef0123456789abcdef");
        String plaintext = "Sensitive PII data 12345";
        CryptoService.EncryptResult result = svc.encrypt(plaintext);
        assertNotNull(result.ciphertext());
        assertNotNull(result.iv());
        String decrypted = svc.decrypt(result.ciphertext(), result.iv());
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachCall() {
        CryptoService svc = service("0123456789abcdef0123456789abcdef");
        String plaintext = "same input";
        CryptoService.EncryptResult r1 = svc.encrypt(plaintext);
        CryptoService.EncryptResult r2 = svc.encrypt(plaintext);
        // IV is random — ciphertexts should differ
        assertFalse(java.util.Arrays.equals(r1.iv(), r2.iv()),
                "Each encryption must use a fresh random IV");
    }

    @Test
    void sha256Hex_producesWellFormedDigest() {
        String digest = CryptoService.sha256Hex("abc");
        assertEquals(64, digest.length(), "SHA-256 hex digest must be 64 characters");
        assertTrue(digest.matches("[0-9a-f]+"), "Digest must be lowercase hex");
        // Same input must always produce the same digest (deterministic)
        assertEquals(digest, CryptoService.sha256Hex("abc"));
        // Different inputs must produce different digests
        assertNotEquals(digest, CryptoService.sha256Hex("xyz"));
    }

    @Test
    void sha256Hex_emptyString_isStable() {
        String d1 = CryptoService.sha256Hex("");
        String d2 = CryptoService.sha256Hex("");
        assertEquals(d1, d2);
        assertEquals(64, d1.length());
    }
}
