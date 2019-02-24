package org.interledger.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link InterledgerResponsePacketMapper}.
 */
public class InterledgerResponsePacketMapperTest {

  private InterledgerResponsePacket fulfillPacket;
  private InterledgerResponsePacket rejectPacket;
  private InterledgerResponsePacket expiredPacket;

  @Before
  public void setup() {
    fulfillPacket = InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();

    rejectPacket =
        InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
            .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
            .message("rejected!")
            .build();

    expiredPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT)
        .message("Timed out!")
        .build();
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
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.T00_INTERNAL_ERROR));
        return Boolean.TRUE;
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
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT));
        return Boolean.TRUE;
      }

    }.map(expiredPacket);

    assertThat(result, is(Boolean.TRUE));
  }
}