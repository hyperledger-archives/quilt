package org.interledger.cryptoconditions.utils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Utility class for encoding and decoding a {@link BigInteger} as a byte array without sign
 * prefix.
 *
 * @author adrianhopebailie
 */
public class UnsignedBigInteger {

  /**
   * Get a positive {@link BigInteger} encoded as a byte array with no sign-prefix.
   *
   * @param value a positive BigInteger value
   * @return input value encoded as a byte[] with leading 0x00 prefix trimmed.
   * @throws IllegalArgumentException if the input value is &lt; 0
   */
  public static byte[] toUnsignedByteArray(BigInteger value) {

    byte[] signedValue = value.toByteArray();
    if (signedValue[0] == 0x00) {
      return Arrays.copyOfRange(signedValue, 1, signedValue.length);
    }

    return signedValue;
  }

  /**
   * Get {@link BigInteger} from byte encoding that assumes the value is  &gt; 0.
   *
   * @param value a byte encoded integer
   * @return a positive {@link BigInteger}
   */
  public static BigInteger fromUnsignedByteArray(byte[] value) {
    return new BigInteger(1, value);
  }

}
