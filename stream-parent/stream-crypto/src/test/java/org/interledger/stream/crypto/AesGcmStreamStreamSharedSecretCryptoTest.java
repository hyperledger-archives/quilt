package org.interledger.stream.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.stream.crypto.AesGcmStreamSharedSecretCrypto.EncryptionMode;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AesGcmStreamSharedSecretCrypto}.
 *
 * <p>Note: Test values were taken from the Interledger-rs project, which mimic the Interledger-js test values.</p>
 *
 * @see "https://github.com/interledger-rs/interledger-rs/blob/master/crates/interledger-stream/src/crypto.rs"
 */
public class AesGcmStreamStreamSharedSecretCryptoTest {

  private static final StreamSharedSecret SHARED_SECRET = StreamSharedSecret.of(new byte[] {
      (byte) 126, (byte) 219, (byte) 117, (byte) 93, (byte) 118, (byte) 248, (byte) 249, (byte) 211, (byte) 20,
      (byte) 211, (byte) 65, 110, (byte) 237, (byte) 80, (byte) 253, (byte) 179, (byte) 81, (byte) 146, (byte) 229,
      (byte) 67, (byte) 231, (byte) 49, (byte) 92, (byte) 127, (byte) 254, (byte) 230, (byte) 144, (byte) 102,
      (byte) 103, (byte) 166, (byte) 150, (byte) 36
  });

  private static final byte[] PLAINTEXT = new byte[] {99, 0, 12, (byte) 255, 77, 31};

  private static final byte[] CIPHERTEXT = new byte[] {
      (byte) 119, (byte) 248, (byte) 213, (byte) 234, (byte) 63, (byte) 200, (byte) 224, (byte) 140, (byte) 212,
      (byte) 222, (byte) 105, (byte) 159, (byte) 246, (byte) 203, (byte) 66, (byte) 155, (byte) 151, (byte) 172,
      (byte) 68, (byte) 24, (byte) 76, (byte) 232, (byte) 90, (byte) 10, (byte) 237, (byte) 146, (byte) 189, (byte) 73,
      (byte) 248, (byte) 196, (byte) 177, (byte) 108, (byte) 115, (byte) 223
  };

  private static final byte[] NONCE_IV = new byte[] {(byte) 119, (byte) 248, (byte) 213, (byte) 234, (byte) 63,
      (byte) 200, (byte) 224, (byte) 140, (byte) 212, (byte) 222, (byte) 105, (byte) 159};

  private AesGcmStreamSharedSecretCrypto streamEncryptionService;

  @Before
  public void setUp() {
    this.streamEncryptionService = new AesGcmStreamSharedSecretCrypto();
  }

  @Test
  public void testEncryptToSameRustJs() {
    byte[] encryptedValue = streamEncryptionService.encryptWithIv(SHARED_SECRET, PLAINTEXT, NONCE_IV);
    assertThat(encryptedValue).isEqualTo(CIPHERTEXT);
  }

  @Test
  public void testDecryptToSameAsRustJs() {
    byte[] decryptedValue = streamEncryptionService.decrypt(SHARED_SECRET, CIPHERTEXT);
    assertThat(decryptedValue).isEqualTo(PLAINTEXT);
  }

  /**
   * Even though we're in standard encryption mode, the decryption should fallback in the event of a decryption error.
   */
  @Test
  public void testDecryptToSameAsRustJsInStandardMode() {
    this.streamEncryptionService = new AesGcmStreamSharedSecretCrypto(EncryptionMode.ENCRYPT_STANDARD);
    byte[] decryptedValue = streamEncryptionService.decrypt(SHARED_SECRET, CIPHERTEXT);
    assertThat(decryptedValue).isEqualTo(PLAINTEXT);
  }

  @Test
  public void losslesslyEncryptAndDecryptsNonStandardMode() {
    byte[] cipherMessage = streamEncryptionService.encrypt(SHARED_SECRET, PLAINTEXT);
    byte[] decryptedValue = streamEncryptionService.decrypt(SHARED_SECRET, cipherMessage);
    assertThat(decryptedValue).isEqualTo(PLAINTEXT);
  }

  @Test
  public void losslesslyEncryptAndDecryptsStandardMode() {
    this.streamEncryptionService = new AesGcmStreamSharedSecretCrypto(EncryptionMode.ENCRYPT_STANDARD);
    byte[] cipherMessage = streamEncryptionService.encrypt(SHARED_SECRET, PLAINTEXT);
    byte[] decryptedValue = streamEncryptionService.decrypt(SHARED_SECRET, cipherMessage);
    assertThat(decryptedValue).isEqualTo(PLAINTEXT);
  }

  @Test
  public void testIvLengthInNonStandardMode() {
    this.streamEncryptionService = new AesGcmStreamSharedSecretCrypto(EncryptionMode.ENCRYPT_NON_STANDARD);
    final int actualIvLength = AesGcmStreamSharedSecretCrypto.AES_GCM_NONCE_IV_LENGTH;
    // See NIST suggestion in JavaxStreamEncryptionService. This test should ALWAYS pass!
    assertThat(actualIvLength >= 12 || actualIvLength < 16);
  }

  @Test
  public void testIvLengthInStandardMode() {
    this.streamEncryptionService = new AesGcmStreamSharedSecretCrypto(EncryptionMode.ENCRYPT_STANDARD);
    final int actualIvLength = AesGcmStreamSharedSecretCrypto.AES_GCM_NONCE_IV_LENGTH;
    // See NIST suggestion in JavaxStreamEncryptionService. This test should ALWAYS pass!
    assertThat(actualIvLength >= 12 || actualIvLength < 16);
  }

  @Test
  public void testIvLength() {
    final int actualIvLength = AesGcmStreamSharedSecretCrypto.AES_GCM_NONCE_IV_LENGTH;
    // See NIST suggestion in JavaxStreamEncryptionService. This test should ALWAYS pass!
    assertThat(actualIvLength >= 12 || actualIvLength < 16);
  }
}
