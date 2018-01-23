package org.interledger.transport.psk;

import org.interledger.InterledgerRuntimeException;

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

/**
 * Convenience methods for encrypting and decrypting PSK messages.
 */
public class PskCryptoUtils {

  /* the expected length of the auth tag, in bytes */
  public static final int AUTH_TAG_LEN_BYTES = 16;
  /* the expected AES key length (256 bits), in bytes */
  public static final int AES_KEY_LEN_BYTES = 256 / 8;
  /* the expected length of the nonce, in bytes */
  public static final int NONCE_LEN_BYTES = 16;

  /*
   * the cipher spec used for encryption and decryption. Note that the RFC calls for PKCS-7, which
   * wikipedia claims is equivalent to PKCS-5
   */
  public static final String CIPHER_SPEC = "AES/GCM/PKCS5Padding";


  /**
   * Encrypts a block of data using the encryption scheme specific in the PSK RFC and returns the
   * encrypted data and the authentication.
   *
   * <p>NOTE: May throw an InvalidKeyException if the Java Cryptography Extension (JCE) Unlimited
   * Strength Jurisdiction Policy Files are not installed.
   *
   * <p>@see <a
   * href="http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html">
   * http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html</a>
   *
   * @param key   The AES encryption key.
   * @param nonce The 16 byte nonce to use as an IV.
   * @param data  The data to encrypt.
   *
   * @return The encrypted data and its accompanying GCM authentication tag.
   *
   * @throws RuntimeException if there is an error encrypting the data
   */
  public static AesGcmEncryptResult encryptPskData(SecretKey key, byte[] nonce, byte[] data) {

    Objects.requireNonNull(key, "cannot encrypt data without the key");
    Objects.requireNonNull(nonce, "cannot encrypt data without the nonce");
    Objects.requireNonNull(data, "cannot encrypt null data");

    Cipher cipher;
    try {
      cipher = Cipher.getInstance(CIPHER_SPEC);

      /* convert the auth tag length to bits */
      GCMParameterSpec spec = new GCMParameterSpec(AUTH_TAG_LEN_BYTES * 8, nonce);

      cipher.init(Cipher.ENCRYPT_MODE, key, spec);

      byte[] encryptOutput = cipher.doFinal(data);

      /*
       * extract the auth tag, see
       * http://stackoverflow.com/questions/23864440/aes-gcm-implementation-with-authentication-tag-
       * in -java#26370130
       */
      final byte[] authTag = Arrays.copyOfRange(encryptOutput,
          encryptOutput.length - AUTH_TAG_LEN_BYTES, encryptOutput.length);

      /*
       * remove the auth tag of the encrypted data, since other implementations probably wont
       * understand proprietary java weirdness..
       */
      final byte[] encryptedData =
          Arrays.copyOf(encryptOutput, encryptOutput.length - AUTH_TAG_LEN_BYTES);

      return new AesGcmEncryptResult() {

        @Override
        public byte[] getEncryptedData() {
          return encryptedData;
        }

        @Override
        public byte[] getAuthenticationTag() {
          return authTag;
        }
      };
    } catch (BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
        | InvalidAlgorithmParameterException | IllegalBlockSizeException e) {
      throw new InterledgerRuntimeException("Error encrypting data of PSK message.", e);
    } catch (InvalidKeyException e) {
      throw new InterledgerRuntimeException("Error encrypting data of PSK message. "
          + "Ensure Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files "
          + "are installed to allow for 256-bit AES keys.", e);
    }
  }

  /**
   * Decrypts an encrypted data block of a PSK message, according to the rules in the PSK RFC.
   *
   * @param key           The AES encryption key.
   * @param authTag       The 16 byte auth tag to verify.
   * @param nonce         The 16 byte nonce to use as an IV.
   * @param encryptedData The data to decrypt.
   *
   * @return The decrypted data.
   */
  public static byte[] decryptPskData(SecretKey key, byte[] authTag, byte[] nonce,
      byte[] encryptedData) {

    Objects.requireNonNull(key, "cannot decrypt data without the key");
    Objects.requireNonNull(authTag, "cannot decrypt data without the authTag");
    Objects.requireNonNull(nonce, "cannot decrypt data without the nonce");
    Objects.requireNonNull(encryptedData, "cannot decrypt null data");

    if (nonce.length != NONCE_LEN_BYTES) {
      throw new InterledgerRuntimeException(
          "Invalid PSK message - nonce must be " + NONCE_LEN_BYTES);
    }

    Cipher cipher;
    try {
      cipher = Cipher.getInstance(CIPHER_SPEC);
      GCMParameterSpec spec = new GCMParameterSpec(AUTH_TAG_LEN_BYTES * 8, nonce);
      cipher.init(Cipher.DECRYPT_MODE, key, spec);

      /*
       * the GCM java implementation has some weirdness to make it fit with the broad crypto api. To
       * validate the data, we must append the authentication tag to the end of the encrypted data
       */
      cipher.update(encryptedData);

      /* this should decrypt the data and verify the authentication tag at the same time */
      return cipher.doFinal(authTag);
    } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException
        | BadPaddingException | NoSuchAlgorithmException e) {
      throw new InterledgerRuntimeException("Error decrypting data of PSK message.", e);
    } catch (InvalidKeyException e) {
      throw new InterledgerRuntimeException("Error decrypting data of PSK message. "
          + "Ensure Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files "
          + "are installed to allow for 256-bit AES keys.", e);
    }

  }

  public interface AesGcmEncryptResult {

    byte[] getEncryptedData();

    byte[] getAuthenticationTag();
  }

}
