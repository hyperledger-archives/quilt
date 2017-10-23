package org.interledger.psk;

import org.interledger.InterledgerRuntimeException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Convenience header representing the public Encryption header found in PSK messages.
 */
public class PskNonceHeader extends PskMessage.Header {

  /* the expected length of the nonce, in bytes */
  private static final int NONCE_LEN_BYTES = 16;

  private final byte[] nonce;

  private PskNonceHeader(final byte[] nonce) {
    super(WellKnown.NONCE, Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(nonce));
    this.nonce = Arrays.copyOf(nonce, nonce.length);
  }

  /**
   * Constructs an instance of the header with a randomly generated value.
   *
   * @return new nonce header
   */
  public static PskNonceHeader seed() {
    try {
      SecureRandom sr = SecureRandom.getInstanceStrong();
      byte[] nonce = new byte[16];
      sr.nextBytes(nonce);
      return new PskNonceHeader(nonce);
    } catch (NoSuchAlgorithmException nsa) {
      throw new InterledgerRuntimeException("Could not generate secure nonce", nsa);
    }
  }

  /**
   * Constructs an instance of the header with the given nonce value.
   *
   * @param nonce The nonce value.
   * @return a {@link PskNonceHeader} instance.
   */
  public static PskNonceHeader fromNonce(byte[] nonce) {
    Objects.requireNonNull(nonce);

    if (nonce.length != NONCE_LEN_BYTES) {
      throw new RuntimeException("Invalid token. Expected 16 bytes.");
    }

    byte[] nonceCopy = Arrays.copyOf(nonce, nonce.length);
    return new PskNonceHeader(nonceCopy);
  }

  /**
   * Constructs an instance of the header with the given header's value interpreted as a base64url
   * encoded nonce.
   *
   * @param header The header value.
   * @return a {@link PskNonceHeader} instance.
   */
  public static PskNonceHeader fromHeader(PskMessage.Header header) {
    Objects.requireNonNull(header);
    return fromNonce(
        Arrays.copyOf(Base64.getUrlDecoder()
            .decode(header.getValue()), NONCE_LEN_BYTES));
  }


  /**
   * Convenience method to retrieve the nonce value.
   * @return The none in {@link byte[]} format.
   */
  public byte[] getNonce() {
    return Arrays.copyOf(nonce, nonce.length);
  }

}
