package org.interledger.transport.psk;

import org.interledger.InterledgerRuntimeException;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This interface represents the message format as defined in the Pre-Shared Key Transport
 * Protocol.
 */
public interface PskMessage {

  String STATUS_LINE = "PSK/1.0";

  /**
   * Get the default builder.
   *
   * @return a {@link Builder} instance.
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * Returns a list of all public headers in the message. Note that all parties may view the public
   * headers.
   * @return all of the <b>public</b> headers, or an empty list.
   */
  List<Header> getPublicHeaders();

  /**
   * Returns the all the <b>public</b> headers with the specified name.
   *
   * @param headerName The name of the header(s) to return
   *
   * @return the <b>public</b> headers whose name matches the parameter, or an empty list.
   */
  List<Header> getPublicHeaders(String headerName);

  /**
   * Returns a list of all <b>private</b> headers in the message, provided that either the message
   * was <b>not</b> encrypted, or the private portion of the message has been decrypted by the
   * receiver.
   *
   * @return the <b>private</b> headers or an empty list.
   */
  List<Header> getPrivateHeaders();

  /**
   * Returns a list of all <b>private</b> headers in the message with the specified name. Note that
   * this depends on either the message <b>not</b> being encrypted, or that the private portion of
   * the message has already been decrypted by the receiver.
   *
   * @param headerName The name of the header(s) to return
   *
   * @return the <b>private</b> headers whose name matches the parameter, or an empty list.
   */
  List<Header> getPrivateHeaders(String headerName);

  /**
   * Get the encryption header.
   *
   * @return the type of encryption used
   */
  PskEncryptionHeader getEncryptionHeader();

  /**
   * Get the nonce header.
   *
   * @return the nonce header (or null if no nonce header is set)
   */
  PskNonceHeader getNonceHeader();

  /**
   * Returns the application data included in the private portion of the message, provided that
   * either the message is not encrypted, or has already been decrypted by the receiver.
   *
   * @return a {@link byte[]} data
   */
  byte[] getData();

  class Header {

    private final String name;
    private final String value;

    public Header(final String name, final String value) {
      this.name = Objects.requireNonNull(name);
      this.value = Objects.requireNonNull(value);
    }

    /**
     * Returns the name of the header.
     *
     * @return A {@link String} instance.
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the value associated with the header.
     *
     * @return A {@link String} instance.
     */
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return name + ":" + value;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !(obj instanceof Header)) {
        return false;
      }

      Header header = (Header) obj;

      return name.equalsIgnoreCase(header.getName())
          && value.equals(header.getValue());
    }

    /**
     * Defines well-known PSK headers.
     */
    public class WellKnown {

      /**
       * The mandatory public Nonce header.
       */
      public static final String NONCE = "Nonce";


      /**
       * The mandatory public Encryption header.
       */
      public static final String ENCRYPTION = "Encryption";

      /**
       * The optional private header to define the expiry of the payment.
       */
      public static final String EXPIRES_AT = "Expires-At";

      /**
       * The optional public header to set the payment id.
       */
      public static final String PAYMENT_ID = "Payment-Id";
    }
  }


  /**
   * A builder for constructing concrete Pre-Shared Key message instances.
   */
  class Builder {

    private final List<Header> publicHeaders;
    private final List<Header> privateHeaders;

    private byte[] data;
    private PskNonceHeader nonceHeader = null;
    private PskEncryptionHeader encryptionHeader = null;


    private Builder() {
      /*
       * we choose array lists to maintain insertion order, and to allow duplicates. the PSK RFC is
       * not explicit, but since its inspired by HTTP which allows duplicates, we presume the same.
       */
      publicHeaders = new ArrayList<>();
      privateHeaders = new ArrayList<>();
    }

    /**
     * Adds a header to the <b>public</b> portion of the PSK message. Note that public headers are
     * visible to all parties transmitting the message.
     *
     * @param header The header to add to the public header portion of the message.
     * @return This {@link Builder} instance.
     */
    public Builder addPublicHeader(final Header header) {
      Objects.requireNonNull(header, "Cannot add null header");

      if (header.getName()
          .equalsIgnoreCase(PskMessage.Header.WellKnown.NONCE)) {
        if (this.nonceHeader != null) {
          throw new InterledgerRuntimeException(
              "Unable to add nonce header. Nonce is already defined.");
        }
        this.nonceHeader = PskNonceHeader.fromHeader(header);
      } else if (header.getName()
          .equalsIgnoreCase(PskMessage.Header.WellKnown.ENCRYPTION)) {
        if (this.encryptionHeader != null) {
          throw new InterledgerRuntimeException(
              "Unable to add encryption header. Encryption is already defined.");
        }
        this.encryptionHeader = PskEncryptionHeader.fromHeader(header);
      }

      publicHeaders.add(header);

      return this;
    }

    /**
     * Adds a header to the <b>public</b> portion of the PSK message. Note that public headers are
     * visible to all parties transmitting the message.
     *
     * @param name  The name of the header.
     * @param value The value associated with the header.
     * @return This {@link Builder} instance.
     */
    public Builder addPublicHeader(final String name, final String value) {
      Objects.requireNonNull(name, "Cannot add null header name");
      Objects.requireNonNull(value, "Cannot add null header value");

      addPublicHeader(new Header(name, value));

      return this;
    }

    /**
     * Adds a header to the <b>private</b> portion of the PSK message. Note that public headers are
     * visible to all parties transmitting the message.
     *
     * @param header The header to add to the public headers in the message.
     * @return This {@link Builder} instance.
     */
    public Builder addPrivateHeader(final Header header) {
      Objects.requireNonNull(header, "Cannot add null header");
      privateHeaders.add(header);
      return this;
    }

    /**
     * Adds a header to the <b>public</b> portion of the PSK message. Note that public headers are
     * visible to all parties transmitting the message. The method will replace
     *
     * @param name  The name of the header.
     * @param value The value associated with the header.
     * @return This {@link Builder} instance.
     */
    public Builder addPrivateHeader(final String name, final String value) {
      Objects.requireNonNull(name, "Cannot add null header name");
      Objects.requireNonNull(value, "Cannot add null header value");

      addPrivateHeader(new Header(name, value));

      return this;
    }

    /**
     * Adds a public "Expires-At" header with the timestamp set to the current time plus the expiry
     * duration.
     *
     * @param expiry The amount of time of now to set the expiry
     *
     * @return This {@link Builder} instance.
     */
    public Builder expiry(TemporalAmount expiry) {
      Objects.requireNonNull(expiry);
      addPrivateHeader(Header.WellKnown.EXPIRES_AT, OffsetDateTime.now()
          .plus(expiry)
          .toString());
      return this;
    }

    /**
     * Adds a public "Payment-Id" header with the given value.
     *
     * @param id The payment id to add asa public header
     *
     * @return This {@link Builder} instance.
     */
    public Builder paymentId(UUID id) {
      Objects.requireNonNull(id);
      addPublicHeader(Header.WellKnown.PAYMENT_ID, id.toString());
      return this;
    }

    /**
     * Sets the decrypted data contained in the body of the PSK message.
     *
     * @param data The data to store in the message.
     * @return This {@link Builder} instance.
     */
    public Builder data(final byte[] data) {
      Objects.requireNonNull(data, "Cannot add null data");
      this.data = data;
      return this;
    }

    /**
     * Convenience method looks at the encryption header that has been added and determines if the
     * message uses encryption or not.
     *
     * <p>This is useful when using the builder during parsing of messages to determine if private
     * headers must be parsed or not.
     *
     * @return true if the message uses encryption
     *
     * @throws NullPointerException if the builder has had no encryption header added.
     */
    public boolean usesEncryption() {
      Objects.requireNonNull(encryptionHeader, "No encryption header added.");
      return (encryptionHeader.getEncryptionType() != PskEncryptionType.NONE);
    }

    /**
     * Builds the PSK message with the data provided, including adding a Nonce header if none is
     * present.
     * @return A new {@link PskMessage} instance.
     */
    public PskMessage build() {
      return new Impl(this);
    }

    /**
     * A concrete implementation of a PSK Message as defined in RFC 16 - Pre-Shared Key Transport
     * Protocol.
     */
    private static final class Impl implements PskMessage {

      private final List<Header> publicHeaders;
      private final PskEncryptionHeader encryptionHeader;
      private final PskNonceHeader nonceHeader;

      private final List<Header> privateHeaders;

      private final byte[] data;

      /**
       * Default constructor for instances of {@link PskMessage}.
       */
      public Impl(Builder builder) {

        publicHeaders = new ArrayList<>(builder.publicHeaders);
        privateHeaders = new ArrayList<>(builder.privateHeaders);

        if (builder.nonceHeader == null) {
          nonceHeader = PskNonceHeader.seed();
          publicHeaders.add(nonceHeader);
        } else {
          nonceHeader = builder.nonceHeader;
        }

        if (builder.encryptionHeader == null) {
          encryptionHeader = PskEncryptionHeader.none();
          publicHeaders.add(encryptionHeader);
        } else {
          encryptionHeader = builder.encryptionHeader;
        }

        if (encryptionHeader.getEncryptionType() != PskEncryptionType.NONE
            && privateHeaders.size() > 0) {
          throw new InterledgerRuntimeException(
              "Can't build an encrypted message with private headers");
        }
        if (builder.data == null) {
          data = new byte[]{};
        } else {
          data = Arrays.copyOf(builder.data, builder.data.length);
        }

      }

      @Override
      public List<Header> getPublicHeaders() {
        return new ArrayList<>(publicHeaders);
      }

      @Override
      public List<Header> getPublicHeaders(final String headerName) {
        return publicHeaders.stream()
            .filter(h -> h.getName()
                .equalsIgnoreCase(headerName))
            .collect(Collectors.toList());
      }

      @Override
      public List<Header> getPrivateHeaders() {
        return new ArrayList<>(privateHeaders);
      }

      @Override
      public List<Header> getPrivateHeaders(String headerName) {
        return privateHeaders.stream()
            .filter(h -> h.getName()
                .equalsIgnoreCase(headerName))
            .collect(Collectors.toList());
      }

      @Override
      public byte[] getData() {
        return Arrays.copyOf(data, data.length);
      }

      @Override
      public PskEncryptionHeader getEncryptionHeader() {
        return encryptionHeader;
      }

      @Override
      public PskNonceHeader getNonceHeader() {
        return nonceHeader;
      }

    }

  }

}
