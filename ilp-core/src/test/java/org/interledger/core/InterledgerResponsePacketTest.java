package org.interledger.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link InterledgerResponsePacket}.
 */
public class InterledgerResponsePacketTest {

  private static final byte[] EMPTY_DATA = new byte[64];

  private InterledgerResponsePacket fulfillPacket;
  private InterledgerResponsePacket rejectPacket;

  @Before
  public void setup() {
    fulfillPacket = InterledgerFulfillPacket.builder()
        .fulfillment(InterledgerFulfillment.of(new byte[32]))
        .data(EMPTY_DATA)
        .build();

    rejectPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message("rejected!")
        .data(EMPTY_DATA)
        .build();
  }

  @Test
  public void validFields() {
    assertThat(fulfillPacket.getData()).isEqualTo(EMPTY_DATA);
    assertThat(rejectPacket.getData()).isEqualTo(EMPTY_DATA);
  }

  @Test
  public void handleFulfill() {
    final AtomicBoolean fulfillCalled = new AtomicBoolean();

    fulfillPacket.handle(
        (fulfillPacket) -> fulfillCalled.set(true),
        (rejectPacket) -> fail("Reject handler should not be called.")
    );

    assertThat(fulfillCalled.get()).isTrue();
  }

  @Test
  public void handleReject() {
    final AtomicBoolean rejectCalled = new AtomicBoolean();

    rejectPacket.handle(
        (fulfillPacket) -> fail("Fulfill handler should not be called."),
        (rejectPacket) -> {
          rejectCalled.set(true);
          assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR);
        }
    );

    assertThat(rejectCalled.get()).isTrue();
  }

  @Test
  public void handleAndReturnFulfill() {
    final AtomicBoolean fulfillCalled = new AtomicBoolean();

    InterledgerResponsePacket response = fulfillPacket.handleAndReturn(
        (fulfillPacket) -> fulfillCalled.set(true),
        (rejectPacket) -> fail("Reject handler should not be called.")
    );

    assertThat(fulfillCalled.get()).isTrue();
    assertThat(response).isEqualTo(fulfillPacket);
  }

  @Test
  public void handleAndReturnReject() {
    final AtomicBoolean rejectCalled = new AtomicBoolean();

    InterledgerResponsePacket response = rejectPacket.handleAndReturn(
        (fulfillPacket) -> fail("Fulfill handler should not be called."),
        (rejectPacket) -> {
          rejectCalled.set(true);
          assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR);
        }
    );

    assertThat(rejectCalled.get()).isTrue();
    assertThat(response).isEqualTo(rejectPacket);
  }

  @Test
  public void testMap() {
    assertThat((Integer) fulfillPacket.map((fulfillPacket) -> 1, (rejectPacket) -> 2)).isEqualTo(1);
    assertThat((Integer) rejectPacket.map((fulfillPacket) -> 1, (rejectPacket) -> 2)).isEqualTo(2);
  }

}
