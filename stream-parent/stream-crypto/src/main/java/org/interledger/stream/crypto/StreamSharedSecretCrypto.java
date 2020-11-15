package org.interledger.stream.crypto;

/**
 * Defines methods for performing low-level encryption and decryption using {@link StreamSharedSecret} for purposes of the
 * STREAM protocol, which relies upon a mutally shared-secret (generally obtained via SPSP) in order to encrypt and
 * decrypt the contents of a STREAM connection.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md#51-encryption"
 */
public interface StreamSharedSecretCrypto {

  /**
   * Encrypt {@code plainText} using the underlying key-store of this implementation.
   *
   * @param streamSharedSecret A byte array containing the secret STREAM connection value shared between sender and *
   *                     receiver.
   * @param plainText    A byte-array containing the plaintext value to encrypt.
   *
   * @return An byte array containing {@code plainText}, but in encrypted form.
   */
  byte[] encrypt(final StreamSharedSecret streamSharedSecret, final byte[] plainText);

  /**
   * Decrypt the supplied {@code cipherText}.
   *
   * @param streamSharedSecret  A byte array containing the secret STREAM connection value shared between sender and
   *                      receiver.
   * @param cipherMessage A byte array containing encrypted ciphertext and an IV/nonce.
   *
   * @return A byte array containing the unencrypted secret value, in binary form.
   */
  byte[] decrypt(final StreamSharedSecret streamSharedSecret, final byte[] cipherMessage);

}
