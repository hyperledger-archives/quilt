package org.interledger.transport.psk;

import org.interledger.InterledgerRuntimeException;

import org.junit.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Test that locally installed provider is working as expected.
 *
 * <p>To use 256-bit AES keys you must have Java Cryptography Extension (JCE) Unlimited Strength
 * Jurisdiction Policy Files installed.
 *
 * <p>@see <a
 * href="http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html">
 * http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html</a>
 */
public class Aes256BitKeysizeTest {

  @Test
  public final void test256bitKey()
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

    SecureRandom sr = SecureRandom.getInstanceStrong();
    byte[] nonce = new byte[16];
    sr.nextBytes(nonce);

    KeyGenerator keygen = KeyGenerator.getInstance("AES");
    keygen.init(256);
    byte[] key = keygen.generateKey().getEncoded();

    byte[] data = new byte[256];
    sr.nextBytes(data);

    Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
    GCMParameterSpec paramSpec = new GCMParameterSpec(128, nonce);

    try {
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), paramSpec);
      cipher.doFinal(data);
    } catch (InvalidKeyException e) {
      throw new InterledgerRuntimeException("Error loading 256bit key. "
          + "Likley cause is missing Unlimited Strength Jurisdiction Policy Files.", e);
    }

  }

}
