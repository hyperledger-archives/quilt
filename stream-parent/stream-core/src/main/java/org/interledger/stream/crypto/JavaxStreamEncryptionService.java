package org.interledger.stream.crypto;

import org.interledger.core.SharedSecret;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An {@link StreamEncryptionService} that uses a JavaKeystore for underlying key storage.
 */
public class JavaxStreamEncryptionService implements StreamEncryptionService {

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

  @Override
  public byte[] encrypt(final SharedSecret sharedSecret, final byte[] plainText) throws EncryptionException {
    Objects.requireNonNull(plainText);

    // Create an initialization vector. For GCM a 12 byte random byte-array is recommend by NIST
    // because it's faster and more secure. See here, page 8:
    // https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf
    final byte[] iv = Random.randBytes(AES_GCM_NONCE_IV_LENGTH);

    return encryptWithIv(sharedSecret, plainText, iv);
  }

  @VisibleForTesting
  byte[] encryptWithIv(final SharedSecret sharedSecret, final byte[] plainText, final byte[] iv)
      throws EncryptionException {
    Objects.requireNonNull(plainText);

    byte[] encryptionKey = Hashing.hmacSha256(sharedSecret.key()).hashBytes(ENCRYPTION_KEY_STRING).asBytes();
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

      // TODO: Fix this in JS and Rust so that the typical ordering is not wonky, and avoids this copy.
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

  @Override
  public byte[] decrypt(final SharedSecret sharedSecret, final byte[] cipherMessage) {
    Objects.requireNonNull(cipherMessage);

    byte[] encryptionKey = Hashing.hmacSha256(sharedSecret.key()).hashBytes(ENCRYPTION_KEY_STRING).asBytes();
    final SecretKey typedEncryptionKey = new SecretKeySpec(encryptionKey, "AES");

    // First, deconstruct the message
    try {
      ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
      byte[] iv = new byte[AES_GCM_NONCE_IV_LENGTH];
      byteBuffer.get(iv);
      byte[] cipherText = new byte[byteBuffer.remaining()];
      byteBuffer.get(cipherText);

      // TODO: See https://github.com/hyperledger/quilt/issues/237
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

}
