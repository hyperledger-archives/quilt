package org.interledger.stream.crypto;

import java.security.SecureRandom;

/**
 * A simple wrapper of {@link SecureRandom}.
 *
 * @since 1.0.0
 */
public final class Random {

  private static final ThreadLocal<SecureRandom> localRandom = new ThreadLocal<SecureRandom>() {
    @Override
    protected SecureRandom initialValue() {
      return newDefaultSecureRandom();
    }
  };

  private static SecureRandom newDefaultSecureRandom() {
    SecureRandom retval = new SecureRandom();
    retval.nextLong(); // force seeding
    return retval;
  }

  /**
   * @return a random byte array of size {@code size}.
   */
  public static byte[] randBytes(int size) {
    byte[] rand = new byte[size];
    localRandom.get().nextBytes(rand);
    return rand;
  }

  public static final int randInt(int max) {
    return localRandom.get().nextInt(max);
  }

  public static final int randInt() {
    return localRandom.get().nextInt();
  }
}
