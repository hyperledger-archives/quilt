package org.interledger.stream.crypto;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An {@link StreamSharedSecretCrypto} that uses a JavaKeystore for underlying key storage.
 */
public class AesGcmStreamSharedSecretCrypto implements StreamSharedSecretCrypto {

  /**
   * For GCM a 12 byte random byte-array is recommend by NIST because it's faster and more secure (See page 8 in the PDF
   * document below).
   *
   * @see "https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf"
   */
  @VisibleForTesting
  static final int AES_GCM_NONCE_IV_LENGTH = 12;

  private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
  private static final byte[] ENCRYPTION_KEY_STRING = "ilp_stream_encryption".getBytes(StandardCharsets.US_ASCII);
  private static final int AUTH_TAG_LENGTH_BITS = 128;
  private static final int AUTH_TAG_LENGTH_BYTES = AUTH_TAG_LENGTH_BITS / 8;
  private final EncryptionMode encryptionMode;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * No-args Constructor that initializes {@link #encryptionMode} to be {@link EncryptionMode#ENCRYPT_NON_STANDARD}
   * because this is the default because it's currently the most widely deploy mechanism.
   */
  public AesGcmStreamSharedSecretCrypto() {
    this(EncryptionMode.ENCRYPT_NON_STANDARD);
  }

  /**
   * Required-args Constructor.
   *
   * @param encryptionMode The {@link EncryptionMode} to use when encrypting and decrypting.
   */
  public AesGcmStreamSharedSecretCrypto(final EncryptionMode encryptionMode) {
    this.encryptionMode = Objects.requireNonNull(encryptionMode);
  }

  @Override
  public byte[] encrypt(final StreamSharedSecret streamSharedSecret, final byte[] plainText)
    throws EncryptionException {
    Objects.requireNonNull(streamSharedSecret);
    Objects.requireNonNull(plainText);

    // Create an initialization vector. For GCM a 12 byte random byte-array is recommend by NIST
    // because it's faster and more secure. See here, page 8:
    // https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf
    final byte[] iv = Random.randBytes(AES_GCM_NONCE_IV_LENGTH);

    return encryptWithIv(streamSharedSecret, plainText, iv);
  }

  @VisibleForTesting
  byte[] encryptWithIv(final StreamSharedSecret streamSharedSecret, final byte[] plainText, final byte[] iv)
    throws EncryptionException {
    Objects.requireNonNull(streamSharedSecret);
    Objects.requireNonNull(plainText);
    Objects.requireNonNull(iv);

    if (this.encryptionMode == EncryptionMode.ENCRYPT_NON_STANDARD) {
      return this.nonStandardModeEncryptWithIv(streamSharedSecret, plainText, iv);
    } else {
      return this.standardModeEncryptWithIv(streamSharedSecret, plainText, iv);
    }
  }

  /**
   * <p>Encrypts {@code plainText} with {@code iv} using the standard byte arrangement of the ciphertext where the
   * AuthTag goes last, as specified by NIST.</p>
   *
   * @param streamSharedSecret A {@link StreamSharedSecret} used for encryption.
   * @param plainText          A byte-array to encrypt.
   * @param iv                 An initialization vector used AES/GCM.
   *
   * @return A byte-array containing encrypted cipherMessage, which consists of the iv plus ciphertext (note this is
   *   inverted from the NIST specification).
   *
   * @see "https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf"
   */
  private byte[] standardModeEncryptWithIv(
    final StreamSharedSecret streamSharedSecret, final byte[] plainText, final byte[] iv
  ) throws EncryptionException {
    Objects.requireNonNull(plainText);
    Preconditions.checkArgument(iv.length == AES_GCM_NONCE_IV_LENGTH);

    byte[] encryptionKey = Hashing.hmacSha256(streamSharedSecret.key()).hashBytes(ENCRYPTION_KEY_STRING).asBytes();
    final SecretKey typedEncryptionKey = new SecretKeySpec(encryptionKey, "AES");

    // See https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
    try {
      final Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
      // 128 is the recommended authentication tag length for GCM. More info can be found in pdf mentioned above.
      // After each encryption operation using GCM mode, callers should re-initialize the cipher objects with GCM
      // parameters using a different IV value.
      final GCMParameterSpec parameterSpec = new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.ENCRYPT_MODE, typedEncryptionKey, parameterSpec);
      byte[] cipherText = cipher.doFinal(plainText);

      // Concatenate to a single message
      final ByteBuffer cipherMessageByteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
      cipherMessageByteBuffer.put(iv);
      cipherMessageByteBuffer.put(cipherText);
      final byte[] cipherMessage = cipherMessageByteBuffer.array();

      // It is best practice to try to wipe sensible data like a cryptographic key or IV from memory as fast as
      // possible. Since Java is a language with automatic memory management, we don’t have any guarantees that the
      // following works as intended, but it should in most cases
      Arrays.fill(iv, (byte) 0); //overwrite the content of key with zeros
      Arrays.fill(encryptionKey, (byte) 0);

      return cipherMessage;
    } catch (NoSuchAlgorithmException
      | NoSuchPaddingException
      | InvalidAlgorithmParameterException
      | InvalidKeyException
      | BadPaddingException
      | IllegalBlockSizeException e
    ) {
      throw new EncryptionException("Unable to Encrypt: ", e);
    }
  }

  /**
   * <p>Encrypts {@code plainText} with {@code iv} using a non-standard byte arrangement where the AuthTag goes first,
   * followed by ciphertext.</p>
   *
   * <p>This arrangement was introduced in the JS and Rust Connectors by mistake, and by the time Java was implemented,
   * JS and Rust were widely deployed enough that Java had to follow suit. While this implementation is non-standard, it
   * isn't technically broken, and  works as long as everyone uses the same arrangement when encrypting/decrypting,
   * which is the case in most Interledger deployments.</p>
   *
   * @param streamSharedSecret A {@link StreamSharedSecret} used for encryption.
   * @param plainText          A byte-array to encrypt.
   * @param iv                 An initialization vector used AES/GCM.
   *
   * @return A byte-array containing encrypted cipherMessage, which consists of the iv plus ciphertext (note this is
   *   inverted from the NIST specification).
   *
   * @see "https://github.com/hyperledger/quilt/issues/237"
   * @deprecated This method will be removed in a future version. Prefer {@link #standardModeEncryptWithIv(StreamSharedSecret,
   *   byte[], byte[])} instead.
   */
  @Deprecated
  private byte[] nonStandardModeEncryptWithIv(
    final StreamSharedSecret streamSharedSecret, final byte[] plainText, final byte[] iv
  ) throws EncryptionException {
    Objects.requireNonNull(plainText);
    Preconditions.checkArgument(iv.length == AES_GCM_NONCE_IV_LENGTH);

    byte[] encryptionKey = Hashing.hmacSha256(streamSharedSecret.key()).hashBytes(ENCRYPTION_KEY_STRING).asBytes();
    final SecretKey typedEncryptionKey = new SecretKeySpec(encryptionKey, "AES");

    // See https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
    try {
      final Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
      // 128 is the recommended authentication tag length for GCM. More info can be found in pdf mentioned above.
      // After each encryption operation using GCM mode, callers should re-initialize the cipher objects with GCM
      // parameters using a different IV value.
      final GCMParameterSpec parameterSpec = new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.ENCRYPT_MODE, typedEncryptionKey, parameterSpec);
      byte[] cipherText = cipher.doFinal(plainText);

      byte[] rearrangedCipherText = new byte[cipherText.length];
      // Rearrange the bytes so that the tag goes first (should have put it last in the JS implementation, but oh well)
      System.arraycopy(
        cipherText, cipherText.length - AUTH_TAG_LENGTH_BYTES, rearrangedCipherText, 0, AUTH_TAG_LENGTH_BYTES
      );
      // Put the cipherText last...
      System.arraycopy(
        cipherText, 0, rearrangedCipherText, AUTH_TAG_LENGTH_BYTES, cipherText.length - AUTH_TAG_LENGTH_BYTES
      );

      // Concatenate to a single message
      final ByteBuffer cipherMessageByteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
      cipherMessageByteBuffer.put(iv);
      cipherMessageByteBuffer.put(rearrangedCipherText);
      final byte[] cipherMessage = cipherMessageByteBuffer.array();

      // It is best practice to try to wipe sensible data like a cryptographic key or IV from memory as fast as
      // possible. Since Java is a language with automatic memory management, we don’t have any guarantees that the
      // following works as intended, but it should in most cases
      Arrays.fill(iv, (byte) 0); //overwrite the content of key with zeros
      Arrays.fill(encryptionKey, (byte) 0);

      return cipherMessage;
    } catch (NoSuchAlgorithmException |
      NoSuchPaddingException |
      InvalidAlgorithmParameterException |
      InvalidKeyException |
      BadPaddingException |
      IllegalBlockSizeException e
    ) {
      throw new EncryptionException("Unable to Encrypt: ", e);
    }
  }

  @Override
  public byte[] decrypt(final StreamSharedSecret streamSharedSecret, final byte[] cipherMessage) {
    Objects.requireNonNull(streamSharedSecret);
    Objects.requireNonNull(cipherMessage);

    if (this.encryptionMode == EncryptionMode.ENCRYPT_NON_STANDARD) {
      try {
        return this.nonStandardModeDecrypt(streamSharedSecret, cipherMessage);
      } catch (EncryptionException e) {
        logger.warn(
          "Unable to decrypt payload in {} mode. Attempting {} mode as a fallback.",
          EncryptionMode.ENCRYPT_NON_STANDARD, EncryptionMode.ENCRYPT_STANDARD
        );
        return this.standardModeDecrypt(streamSharedSecret, cipherMessage);
      }
    } else {
      try {
        return this.standardModeDecrypt(streamSharedSecret, cipherMessage);
      } catch (EncryptionException e) {
        logger.warn(
          "Unable to decrypt payload in {} mode. Attempting {} mode as a fallback.",
          EncryptionMode.ENCRYPT_STANDARD,
          EncryptionMode.ENCRYPT_NON_STANDARD,
          e
        );
        return this.nonStandardModeDecrypt(streamSharedSecret, cipherMessage);
      }
    }
  }

  /**
   * <p>Decrypts {@code cipherMessage} using the standard byte arrangement of the embedded ciphertext where the
   * AuthTag goes last, as specified by NIST.</p>
   *
   * @param streamSharedSecret A {@link StreamSharedSecret} used for encryption.
   * @param cipherMessage      A byte-array to decrypt.
   *
   * @return A byte-array containing decrypted plaintext.
   *
   * @see "https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf"
   */
  private byte[] standardModeDecrypt(final StreamSharedSecret streamSharedSecret, final byte[] cipherMessage) {
    Objects.requireNonNull(cipherMessage);

    byte[] encryptionKey = Hashing.hmacSha256(streamSharedSecret.key()).hashBytes(ENCRYPTION_KEY_STRING).asBytes();
    final SecretKey typedEncryptionKey = new SecretKeySpec(encryptionKey, "AES");

    // First, deconstruct the message
    try {
      ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
      byte[] iv = new byte[AES_GCM_NONCE_IV_LENGTH];
      byteBuffer.get(iv);
      byte[] cipherText = new byte[byteBuffer.remaining()];
      byteBuffer.get(cipherText);

      final Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
      cipher.init(Cipher.DECRYPT_MODE, typedEncryptionKey, new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv));
      byte[] plainText = cipher.doFinal(cipherText);

      // It is best practice to try to wipe sensible data like a cryptographic key or IV from memory as fast as
      // possible. Since Java is a language with automatic memory management, we don’t have any guarantees that the
      // following works as intended, but it should in most cases:
      Arrays.fill(iv, (byte) 0); //overwrite the content of key with zeros
      Arrays.fill(encryptionKey, (byte) 0);

      return plainText;
    } catch (Exception e) {
      throw new EncryptionException(e.getMessage(), e);
    }
  }

  /**
   * <p>Decrypts {@code cipherMessage} using the non-standard byte arrangement of the embedded ciphertext where the
   * AuthTag goes first, which is non-standard but commonly deployed.</p>
   *
   * <p>This arrangement was introduced in the JS and Rust Connectors by mistake, and by the time Java was implemented,
   * JS and Rust were widely deployed enough that Java had to follow suit. While this implementation is non-standard, it
   * isn't technically broken, and  works as long as everyone uses the same arrangement when encrypting/decrypting,
   * which is the case in most Interledger deployments.</p>
   *
   * @param streamSharedSecret A {@link StreamSharedSecret} used for encryption.
   * @param cipherMessage      A byte-array to decrypt.
   *
   * @return A byte-array containing decrypted plaintext.
   *
   * @see "https://github.com/hyperledger/quilt/issues/237"
   * @see "https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf"
   * @deprecated This method will be removed in a future version. Prefer {@link #standardModeDecrypt(StreamSharedSecret,
   *   byte[])} instead.
   */
  @Deprecated
  private byte[] nonStandardModeDecrypt(final StreamSharedSecret streamSharedSecret, final byte[] cipherMessage) {
    Objects.requireNonNull(cipherMessage);

    byte[] encryptionKey = Hashing.hmacSha256(streamSharedSecret.key()).hashBytes(ENCRYPTION_KEY_STRING).asBytes();
    final SecretKey typedEncryptionKey = new SecretKeySpec(encryptionKey, "AES");

    // First, deconstruct the message
    try {
      ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
      byte[] iv = new byte[AES_GCM_NONCE_IV_LENGTH];
      byteBuffer.get(iv);
      byte[] cipherText = new byte[byteBuffer.remaining()];
      byteBuffer.get(cipherText);

      // See https://github.com/hyperledger/quilt/issues/237
      // Rearrange the bytes so that the `tag` goes last, after tha Additionally Authenticated Data (AAD). Prior to this
      // reversal, the data is inverted because that's what the RFC specifies, and that's what JS and Rust do.
      byte[] rearrangedCipherText = new byte[cipherText.length];
      System.arraycopy(
        cipherText, 0, rearrangedCipherText, cipherText.length - AUTH_TAG_LENGTH_BYTES, AUTH_TAG_LENGTH_BYTES
      );
      // Put the cipherText last...
      System.arraycopy(
        cipherText, AUTH_TAG_LENGTH_BYTES, rearrangedCipherText, 0, cipherText.length - AUTH_TAG_LENGTH_BYTES
      );

      final Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
      cipher.init(Cipher.DECRYPT_MODE, typedEncryptionKey, new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv));
      byte[] plainText = cipher.doFinal(rearrangedCipherText);

      // It is best practice to try to wipe sensible data like a cryptographic key or IV from memory as fast as
      // possible. Since Java is a language with automatic memory management, we don’t have any guarantees that the
      // following works as intended, but it should in most cases:
      Arrays.fill(iv, (byte) 0); //overwrite the content of key with zeros
      Arrays.fill(encryptionKey, (byte) 0);

      return plainText;
    } catch (Exception e) {
      throw new EncryptionException(e.getMessage(), e);
    }
  }

  /**
   * Defines the mode this service will operate in, based upon the options founds in issue #237.
   *
   * @see "https://github.com/hyperledger/quilt/issues/237"
   */
  public enum EncryptionMode {
    /**
     * Encrypts payloads with ciphertext that has the AuthTag last, which is non-standard but commonly deployed in JS
     * and RUST implementations. Decryption attempts non-standard-mode first, and falls-back to standard-mode in the
     * event of an {@link EncryptionException}.
     */
    ENCRYPT_NON_STANDARD,

    /**
     * Encrypts payloads with ciphertext that has the AuthTag first, which is implied by the NIST standard but not
     * commonly deployed in JS or RUST implementations. Decryption attempts standard-mode first, and falls-back to
     * non-standard mode in the event of an {@link EncryptionException}.
     */
    ENCRYPT_STANDARD
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", AesGcmStreamSharedSecretCrypto.class.getSimpleName() + "[", "]")
      .add("encryptionMode=" + encryptionMode)
      .toString();
  }
}
