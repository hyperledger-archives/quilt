package org.interledger.stream.receiver.testutils;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.AbstractLink;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

/**
 * <p>Simulates an ILPv4 network by allowing two {@link Link} instances to speak directly to each other (as
 * opposed to actually needing an intermediate network transport). This can be useful when testing end-to-end code, such
 * as STREAM sender/receiver interactions.</p>
 *
 * <p>Ordinarily, an ILPv4 node would utilize a {@link Link} to connect to an Interledger peer
 * Connector/Router. This peer might be connected to an arbitrary number of intermediate nodes, forming a graph of
 * potential connections between the `sender` and `receiver`. In these cases, it can be difficult to isolate various
 * network conditions for testing purposes, such as path-exchange rate, path latency, path distance, and more.</p>
 *
 * <p>In order to isolate and control these variables for testing purposes, this class can be used to allow, for
 * example, a STREAM sender and receiver to interact with each other without actually engaging an actual
 * Connector/Router or Link with an actual underlying network transport.</p>
 *
 * <p>This class works by defining a `left` and a `right` link where each Link's internal methods are connected to
 * the other Link's method to simulate network connectivity.</p>
 * <pre>
 * ┌────────────┬────────────┐               ┌────────────┬────────────┐
 * │            │  SendData  │──────────────▷│   OnData   │            │
 * │            ├────────────┘               └────────────┤            │
 * │ Left Link  │                                         │ Right Link │
 * │            ├────────────┐               ┌────────────┤            │
 * │            │   OnData   │◁──────────────│  SendData  │            │
 * └────────────┴────────────┘               └────────────┴────────────┘
 * </pre>
 */
public class SimulatedILPv4Network {

  private final Link<?> leftToRightLink;
  private final SimulatedPathConditions leftToRightNetworkConditions;

  private final Link<?> rightToLeftLink;
  private final SimulatedPathConditions rightToLeftNetworkConditions;

  private Random random = new SecureRandom();

  /**
   * Required-args Constructor.
   *
   * @param leftToRightNetworkConditions A {@link SimulatedPathConditions} that governs the simulated path from the left
   *                                     {@link Link} to the right {@link Link}.
   * @param rightToLeftNetworkConditions A {@link SimulatedPathConditions} that governs the simulated path from right to
   *                                     left.
   */
  public SimulatedILPv4Network(
      final SimulatedPathConditions leftToRightNetworkConditions,
      final SimulatedPathConditions rightToLeftNetworkConditions
  ) {
    this.leftToRightNetworkConditions = Objects.requireNonNull(leftToRightNetworkConditions);
    this.rightToLeftNetworkConditions = Objects.requireNonNull(rightToLeftNetworkConditions);

    this.leftToRightLink = new AbstractLink<LinkSettings>(
        () -> Link.SELF, LinkSettings.builder().linkType(
        LinkType.of("leftLink")).build()
    ) {
      @Override
      public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
        return sendPacketToRight(preparePacket);
      }
    };
    leftToRightLink.setLinkId(LinkId.of("left"));

    this.rightToLeftLink = new AbstractLink<LinkSettings>(
        () -> Link.SELF,
        LinkSettings.builder().linkType(LinkType.of("rightLink")).build()
    ) {
      @Override
      public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
        return sendPacketToLeft(preparePacket);
      }
    };
    rightToLeftLink.setLinkId(LinkId.of("right"));
  }

  public InterledgerResponsePacket sendPacketToRight(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    // Depending on simulated network conditions, reject packets...
    if (rejectPacket(leftToRightNetworkConditions.packetRejectionPercentage())) {
      return InterledgerRejectPacket.builder()
          .message("Intermediate Connector timed out")
          .triggeredBy(InterledgerAddress.of("example.simulated.ilpv4.network.left-to-right"))
          .code(InterledgerErrorCode.T03_CONNECTOR_BUSY)
          .build();
    } else if (maxPacketAmountExceeded(leftToRightNetworkConditions, preparePacket)) {
      return InterledgerRejectPacket.builder()
          .message("Intermediate Connector does not allow packets greater than "
              + leftToRightNetworkConditions.maxPacketAmount().get()
          )
          .triggeredBy(InterledgerAddress.of("example.simulated.ilpv4.network.left-to-right"))
          .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
          .build();
    } else {
      final InterledgerPreparePacket adjustedPreparePacket = this.applyExchangeRate(
          preparePacket,
          this.leftToRightNetworkConditions.currentExchangeRateSupplier().get()
      );

      return this.leftToRightLink.getLinkHandler()
          .map(linkHandler -> linkHandler.handleIncomingPacket(adjustedPreparePacket))
          .orElseThrow(() -> new RuntimeException("No LinkHandler registered for leftToRightLink"));
    }
  }

  public InterledgerResponsePacket sendPacketToLeft(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    // Depending on simulated network conditions, reject packets...
    if (rejectPacket(rightToLeftNetworkConditions.packetRejectionPercentage())) {
      return InterledgerRejectPacket.builder()
          .message("Intermediate Connector timed out")
          .triggeredBy(InterledgerAddress.of("example.simulated.ilpv4.network.right-to-left"))
          .code(InterledgerErrorCode.T03_CONNECTOR_BUSY)
          .build();
    } else if (maxPacketAmountExceeded(rightToLeftNetworkConditions, preparePacket)) {
      return InterledgerRejectPacket.builder()
          .message("Intermediate Connector does not allow packets greater than "
              + rightToLeftNetworkConditions.maxPacketAmount().get()
          )
          .triggeredBy(InterledgerAddress.of("example.simulated.ilpv4.network.ight-to-left"))
          .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
          .build();
    } else {
      final InterledgerPreparePacket adjustedPreparePacket = this.applyExchangeRate(
          preparePacket,
          this.rightToLeftNetworkConditions.currentExchangeRateSupplier().get()
      );

      return this.rightToLeftLink.getLinkHandler()
          .map(linkHandler -> linkHandler.handleIncomingPacket(adjustedPreparePacket))
          .orElseThrow(() -> new RuntimeException("No LinkHandler registered for leftToRightLink"));
    }
  }

  private InterledgerPreparePacket applyExchangeRate(
      final InterledgerPreparePacket preparePacket, final BigDecimal multiplier
  ) {
    Objects.requireNonNull(preparePacket);
    Objects.requireNonNull(multiplier);

    return InterledgerPreparePacket.builder().from(preparePacket)
        .amount(multiplier.multiply(new BigDecimal(preparePacket.getAmount())).toBigIntegerExact())
        .build();
  }

  public Link<?> getLeftToRightLink() {
    return leftToRightLink;
  }

  public Link<?> getRightToLeftLink() {
    return rightToLeftLink;
  }

  /**
   * Determines if a packet should reject {@code percentage} percent of the time. For example, by supplying a value of
   * `0.4`, then this method will return {@code true} approximately 40% of the time, whereas it will return {@code
   * false} approximately 60% of the time.
   *
   * @param percentage The percentage of time this method should return {@code true}, indicating a reject packet should
   *                   be returned.
   *
   * @return {@code true} if a packet should be rejected; {@code false} if it should be fulfilled.
   */
  private boolean rejectPacket(final float percentage) {
    Preconditions.checkArgument(percentage >= 0.0f);
    Preconditions.checkArgument(percentage <= 1.0f);

    float chance = this.random.nextFloat();
    if (chance <= percentage) {
      // happens {percentage}% of the time...
      return true;
    } else {
      return false;
    }
  }

  /**
   * Determines if the {@code preparePacket} has an amount that is too large for a given network path.
   *
   * @param simulatedPathConditions A {@link SimulatedPathConditions} to check against.
   * @param preparePacket           An {@link InterledgerPreparePacket} to check the size of the amount.
   *
   * @return
   */
  private boolean maxPacketAmountExceeded(
      final SimulatedPathConditions simulatedPathConditions, final InterledgerPreparePacket preparePacket
  ) {
    Objects.requireNonNull(simulatedPathConditions);
    Objects.requireNonNull(preparePacket);
    return UnsignedLong.valueOf(preparePacket.getAmount())
        .compareTo(simulatedPathConditions.maxPacketAmount().get()) > 0;
  }
}
