package org.interledger.core;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

/**
 * Unit test for {@link InterledgerResponsePacketHandler}.
 */
public class InterledgerResponsePacketHandlerTest {

  private Optional<InterledgerResponsePacket> fulfillPacket;
  private Optional<InterledgerResponsePacket> rejectPacket;
  private Optional<InterledgerResponsePacket> expiredPacket;

  @Before
  public void setup() {
    fulfillPacket = Optional.ofNullable(
        InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build()
    );

    rejectPacket = Optional.ofNullable(
        InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
            .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
            .message("rejected!")
            .build()
    );

    expiredPacket = Optional.empty();
  }

  @Test(expected = NullPointerException.class)
  public void handleNullPacket() {
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }

      /**
       * Handle the packet as an {@link InterledgerPacket}.
       */
      @Override
      protected void handleExpiredPacket() {
        throw new RuntimeException("Should not fulfill!");
      }
    }.handle(null);
  }

  @Test
  public void handleFulfillPacket() {
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        // No-op, success!
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }

      /**
       * Handle the packet as an {@link InterledgerPacket}.
       */
      @Override
      protected void handleExpiredPacket() {
        throw new RuntimeException("Should not expire!");
      }
    }.handle(fulfillPacket);
  }

  @Test
  public void handleRejectPacket() {
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        // No-op, success!
      }

      /**
       * Handle the packet as an {@link InterledgerPacket}.
       */
      @Override
      protected void handleExpiredPacket() {
        throw new RuntimeException("Should not expire!");
      }
    }.handle(rejectPacket);
  }

  @Test
  public void handleExpiredPacket() {
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }

      /**
       * Handle the packet as an {@link InterledgerPacket}.
       */
      @Override
      protected void handleExpiredPacket() {
        // No-op, success!
      }
    }.handle(expiredPacket);
  }

}