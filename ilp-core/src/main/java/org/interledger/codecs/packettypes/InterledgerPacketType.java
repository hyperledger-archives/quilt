package org.interledger.codecs.packettypes;

import org.interledger.InterledgerRuntimeException;

import java.net.URI;
import java.util.Objects;

/**
 * An interface that defines how Interledger Packets are typed using ASN.1 OER encoding.
 */
public interface InterledgerPacketType {

  int ILP_PAYMENT_TYPE = 1;
  int ILQP_QUOTE_LIQUIDITY_REQUEST_TYPE = 2;
  int ILQP_QUOTE_LIQUIDITY_RESPONSE_TYPE = 3;
  int ILQP_QUOTE_BY_SOURCE_AMOUNT_REQUEST_TYPE = 4;
  int ILQP_QUOTE_BY_SOURCE_AMOUNT_RESPONSE_TYPE = 5;
  int ILQP_QUOTE_BY_DESTINATION_AMOUNT_REQUEST_TYPE = 6;
  int ILQP_QUOTE_BY_DESTINATION_AMOUNT_RESPONSE_TYPE = 7;
  int INTERLEDGER_PROTOCOL_ERROR = 8;

  /**
   * A helper method that will translate an integer into an instance of {@link
   * InterledgerPacketType}. Note that this method only handled standard Interledger packets types.
   * To operate upon non-standard packets, a different method should be used.
   *
   * @param type The integer type.
   *
   * @return An instance of {@link InterledgerPacketType}.
   *
   * @throws InvalidPacketTypeException If the supplied {@code type} is invalid.
   */
  static InterledgerPacketType fromTypeId(final int type) throws InvalidPacketTypeException {
    switch (type) {
      case ILP_PAYMENT_TYPE:
        return new PaymentPacketType();
      case ILQP_QUOTE_LIQUIDITY_REQUEST_TYPE:
        return new QuoteLiquidityRequestPacketType();
      case ILQP_QUOTE_LIQUIDITY_RESPONSE_TYPE:
        return new QuoteLiquidityResponsePacketType();
      case ILQP_QUOTE_BY_SOURCE_AMOUNT_REQUEST_TYPE:
        return new QuoteBySourceAmountRequestPacketType();
      case ILQP_QUOTE_BY_SOURCE_AMOUNT_RESPONSE_TYPE:
        return new QuoteBySourceAmountResponsePacketType();
      case ILQP_QUOTE_BY_DESTINATION_AMOUNT_REQUEST_TYPE:
        return new QuoteByDestinationAmountRequestPacketType();
      case ILQP_QUOTE_BY_DESTINATION_AMOUNT_RESPONSE_TYPE:
        return new QuoteByDestinationAmountResponsePacketType();
      case INTERLEDGER_PROTOCOL_ERROR:
        return new InterledgerErrorPacketType();
      default:
        throw new InvalidPacketTypeException(
          String.format("%s is an unsupported Packet Type!", type));
    }
  }

  /**
   * The packet's type identifier, as specified by IL-RFC-3.
   *
   * @return An {@link Integer} representing the type of this packet.
   */
  Integer getTypeIdentifier();

  /**
   * A URI representing the formal type of this packet per the Interledger Header Type registry
   * maintained at IANA.
   *
   * @return An instance of {@link String}.
   *
   * @see "http://www.iana.org/assignments/interledger-header-types"
   */
  URI getTypeUri();

  /**
   * An exception that indicates if a packet type is invalid for the current implementation.
   */
  class InvalidPacketTypeException extends InterledgerRuntimeException {

    private static final long serialVersionUID = 6086784345849001539L;

    public InvalidPacketTypeException(String message) {
      super(message);
    }
  }

  /**
   * An abstract implementation of {@link InterledgerPacketType}.
   */
  abstract class AbstractInterledgerPacketType implements InterledgerPacketType {

    private final Integer typeIdentifier;
    private final URI typeUri;

    protected AbstractInterledgerPacketType(final Integer typeIdentifier, final URI typeUri) {
      this.typeIdentifier = Objects.requireNonNull(typeIdentifier);
      this.typeUri = Objects.requireNonNull(typeUri);
    }

    public Integer getTypeIdentifier() {
      return typeIdentifier;
    }

    public URI getTypeUri() {
      return typeUri;
    }

    @Override
    public String toString() {
      return "AbstractInterledgerPacketType{"
        + "typeIdentifier=" + typeIdentifier
        + ", typeUri=" + typeUri
        + '}';
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      AbstractInterledgerPacketType that = (AbstractInterledgerPacketType) obj;

      return typeIdentifier.equals(that.typeIdentifier)
        && typeUri.equals(that.typeUri);
    }

    @Override
    public int hashCode() {
      int result = typeIdentifier.hashCode();
      result = 31 * result + typeUri.hashCode();
      return result;
    }
  }
}
