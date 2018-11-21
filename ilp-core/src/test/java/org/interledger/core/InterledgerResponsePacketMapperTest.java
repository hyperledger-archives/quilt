package org.interledger.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

/**
 * Unit test for {@link InterledgerResponsePacketMapper}.
 */
public class InterledgerResponsePacketMapperTest {

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
  public void mapNullPacket() {
    new InterledgerResponsePacketMapper<Boolean>() {
      @Override
      protected Boolean mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected Boolean mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }

      /**
       * Handle the packet as an {@link InterledgerPacket}.
       */
      @Override
      protected Boolean mapExpiredPacket() {
        throw new RuntimeException("Should not fulfill!");
      }
    }.map(null);
    fail();
  }

  @Test
  public void mapFulfillPacket() {
    final Boolean result = new InterledgerResponsePacketMapper<Boolean>() {
      @Override
      protected Boolean mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        return true;
      }

      @Override
      protected Boolean mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }

      /**
       * Handle the packet as an {@link InterledgerPacket}.
       */
      @Override
      protected Boolean mapExpiredPacket() {
        throw new RuntimeException("Should not expire!");
      }
    }.map(fulfillPacket);

    assertThat(result, is(Boolean.TRUE));
  }

  @Test
  public void mapRejectPacket() {
    final Boolean result = new InterledgerResponsePacketMapper<Boolean>() {
      @Override
      protected Boolean mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected Boolean mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        return Boolean.TRUE;
      }

      /**
       * Handle the packet as an {@link InterledgerPacket}.
       */
      @Override
      protected Boolean mapExpiredPacket() {
        throw new RuntimeException("Should not expire!");
      }
    }.map(rejectPacket);

    assertThat(result, is(Boolean.TRUE));
  }

  @Test
  public void mapExpiredPacket() {
    final Boolean result = new InterledgerResponsePacketMapper<Boolean>() {
      @Override
      protected Boolean mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected Boolean mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }

      /**
       * Handle the packet as an {@link InterledgerPacket}.
       */
      @Override
      protected Boolean mapExpiredPacket() {
        return Boolean.TRUE;
      }
    }.map(expiredPacket);

    assertThat(result, is(Boolean.TRUE));
  }
}