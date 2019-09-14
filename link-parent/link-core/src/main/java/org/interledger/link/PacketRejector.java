package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Utility class for helping to reject a packet.
 */
public class PacketRejector {

  public static final InterledgerAddress UNSET_OPERATOR_ADDRESS =
      InterledgerAddress.of(InterledgerAddress.AllocationScheme.PRIVATE.getValue() + ".unset-operator-address");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} of the ILP address for the node that's operating this Link.
   */
  public PacketRejector(final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier) {
    this.operatorAddressSupplier = Objects
        .requireNonNull(operatorAddressSupplier, "operatorAddressSupplier must not be null");
  }

  /**
   * Helper-method to reject a request.
   *
   * @param rejectingLinkId The {@link Link} that was the source of this Reject action.
   * @param preparePacket   The {@link InterledgerPreparePacket} that was the catalyst for this reject action.
   * @param errorCode       The {@link InterledgerErrorCode} that expresses the reason for this reject action.
   * @param errorMessage    An error message for clarity. If no error message is desired, supply an empty string.
   *
   * @return An {@link InterledgerRejectPacket} constructed using the above inputs.
   */
  public InterledgerRejectPacket reject(
      final LinkId rejectingLinkId, final InterledgerPreparePacket preparePacket,
      final InterledgerErrorCode errorCode, final String errorMessage
  ) {
    Objects.requireNonNull(rejectingLinkId, "rejectingLinkId must not be null");
    Objects.requireNonNull(preparePacket, "preparePacket must not be null");
    Objects.requireNonNull(errorCode, "errorCode must not be null");
    Objects.requireNonNull(errorMessage, "errorMessage must not be null");

    // Reject.
    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
        .triggeredBy(operatorAddressSupplier.get().orElse(UNSET_OPERATOR_ADDRESS))
        .code(errorCode)
        .message(errorMessage)
        .build();

    logger.debug(
        "Rejecting inside linkId={}: PreparePacket={} RejectPacket={}",
        rejectingLinkId, preparePacket, rejectPacket
    );

    return rejectPacket;
  }
}
