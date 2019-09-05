package org.interledger.link.http.auth;

/**
 * <p>Defines a generic interface for decrypting ciphertext values.</p>
 *
 * <p>A primary use-case for this interface is to decrypt properties or values that are sitting in-memory in encrypted
 * form. For example, an encrypted shared-secret signing key may have been loaded from a property file or a datastore.
 * This interface allows implementations to define how to decrypt such information using arbitrary schemes.</p>
 */
public interface Decryptor {

  /**
   * Load an actual secret using the supplied information.
   *
   * @param cipherMessage The byte[] array containing cipherText and other meta-data than can be used to decrypt the
   *                      ciphertext.
   *
   * @return A byte array containing the unencrypted secret value, in binary form.
   */
  byte[] decrypt(final byte[] cipherMessage);
}
