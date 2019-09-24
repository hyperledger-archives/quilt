package org.interledger.stream.receiver.testutils;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.AbstractLink;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;

import java.math.BigDecimal;
import java.util.Objects;

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

  private final Link<?> leftLink;
  private final Link<?> rightLink;

  private final SimulatedPathConditions leftToRightNetworkConditions;
  private final SimulatedPathConditions rightToLeftNetworkConditions;

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

    this.leftLink = new AbstractLink<LinkSettings>(() -> Link.SELF, LinkSettings.builder().linkType(
        LinkType.of("leftLink")).build()) {
      @Override
      public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
        return sendPacketToRight(preparePacket);
      }
    };
    leftLink.setLinkId(LinkId.of("left"));

    this.rightLink = new AbstractLink<LinkSettings>(() -> Link.SELF,
        LinkSettings.builder().linkType(LinkType.of("rightLink")).build()) {
      @Override
      public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
        return sendPacketToLeft(preparePacket);
      }
    };
    rightLink.setLinkId(LinkId.of("right"));
  }

  public InterledgerResponsePacket sendPacketToRight(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    final InterledgerPreparePacket adjustedPreparePacket = this.applyExchangeRate(
        preparePacket,
        this.rightToLeftNetworkConditions.currentExchangeRateSupplier().get()
    );

    return this.rightLink.getLinkHandler().map(linkHandler -> linkHandler.handleIncomingPacket(adjustedPreparePacket))
        .orElseThrow(() -> new RuntimeException("No LinkHandler registered for Right Link"));
  }

  public InterledgerResponsePacket sendPacketToLeft(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    final InterledgerPreparePacket adjustedPreparePacket = this.applyExchangeRate(
        preparePacket,
        this.leftToRightNetworkConditions.currentExchangeRateSupplier().get()
    );

    return this.leftLink.getLinkHandler()
        .map(linkHandler -> linkHandler.handleIncomingPacket(adjustedPreparePacket))
        .orElseThrow(() -> new RuntimeException("No LinkHandler registered for Left Link"));
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

  public Link<?> getLeftLink() {
    return leftLink;
  }

  public Link<?> getRightLink() {
    return rightLink;
  }
}
