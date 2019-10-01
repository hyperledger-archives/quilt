package org.interledger.stream.crypto;

import org.interledger.core.SharedSecret;

/**
 * A helper class for performing encryption and decryption operations required by the STREAM protocol.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md#51-encryption"
 */
public interface StreamEncryptionService {

  /**
   * Encrypt {@code plainText} using the underlying key-store of this implementation.
   *
   * @param sharedSecret A byte array containing the secret STREAM connection value shared between sender and *
   *                     receiver.
   * @param plainText    A byte-array containing the plaintext value to encrypt.
   *
   * @return An byte array containing {@code plainText}, but in encrypted form.
   */
  byte[] encrypt(final SharedSecret sharedSecret, final byte[] plainText);

  /**
   * Decrypt the supplied {@code cipherText}.
   *
   * @param sharedSecret  A byte array containing the secret STREAM connection value shared between sender and
   *                      receiver.
   * @param cipherMessage A byte array containing encrypted ciphertext and an IV/nonce.
   *
   * @return A byte array containing the unencrypted secret value, in binary form.
   */
  byte[] decrypt(final SharedSecret sharedSecret, final byte[] cipherMessage);

}
