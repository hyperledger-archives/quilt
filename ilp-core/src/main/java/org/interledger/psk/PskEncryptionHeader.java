package org.interledger.psk;

import org.interledger.InterledgerRuntimeException;
import org.interledger.psk.PskMessage.Header;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Convenience header representing the public Encryption header found in PSK messages.
 */
public class PskEncryptionHeader extends PskMessage.Header {

  private final byte[] authTag;
  private final PskEncryptionType type;

  private PskEncryptionHeader() {
    super(WellKnown.ENCRYPTION, PskEncryptionType.NONE.toString());
    this.type = PskEncryptionType.NONE;
    this.authTag = null;
  }

  private PskEncryptionHeader(byte[] authTag) {
    super(WellKnown.ENCRYPTION,
        PskEncryptionType.AES_256_GCM.toString()
            + " "
            + Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(authTag));
    this.type = PskEncryptionType.AES_256_GCM;
    this.authTag = Arrays.copyOf(authTag, authTag.length);
  }

  /**
   * Constructs an instance of the header with the encryption type NONE.
   * @return A {@link PskEncryptionHeader} of no encryption type.
   */
  public static PskEncryptionHeader none() {
    return new PskEncryptionHeader();
  }

  /**
   * Constructs an instance of the header with the AES-GCM encryption type and provided
   * authentication tag value.
   *
   * @param authenticationTag The authentication tag value. May be null.
   * @return A {@link PskEncryptionHeader} instance based on the authentication tag if present.
   */
  public static PskEncryptionHeader aesGcm(final byte[] authenticationTag) {
    Objects.requireNonNull(authenticationTag);
    return new PskEncryptionHeader(authenticationTag);
  }

  /**
   * Constructs an instance of the header of an existing header with the appropriate values.
   *
   * @param header An existing header
   *
   * @return An encryption header
   *
   * @throws RuntimeException if an encryption header can't be constructed of the given header
   */
  public static PskEncryptionHeader fromHeader(Header header) {

    String value = header.getValue()
        .trim();

    if (value.equalsIgnoreCase(PskEncryptionType.NONE.toString())) {
      return none();
    }

    String[] tokens = value.split(" ");
    if (tokens[0].equalsIgnoreCase(PskEncryptionType.AES_256_GCM.toString())) {
      if (tokens.length == 1) {
        throw new InterledgerRuntimeException("Invalid AES GCM encryption header. No auth tag.");
      }
      return aesGcm(Base64.getUrlDecoder()
          .decode(tokens[1]));
    }

    throw new InterledgerRuntimeException("Invalid encryption header value.");
  }

  /**
   * Convenience method to retrieve the authentication tag value, if it is present in the header.
   * @return the authentication tag in {@link byte[]} format.
   */
  public byte[] getAuthenticationTag() {
    if (type == PskEncryptionType.NONE) {
      return null;
    }
    return Arrays.copyOf(authTag, authTag.length);
  }

  /**
   * Returns the encryption type indicated in the header.
   * @return The {@link PskEncryptionType} from the header.
   */
  public PskEncryptionType getEncryptionType() {
    return type;
  }

  @Override
  public String getName() {
    return PskMessage.Header.WellKnown.ENCRYPTION;
  }

  @Override
  public String getValue() {
    if (getEncryptionType() == PskEncryptionType.NONE) {
      return PskEncryptionType.NONE.toString();
    }

    return PskEncryptionType.AES_256_GCM.toString() + " "
        + Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(this.authTag);
  }

}
