package org.interledger.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link InterledgerResponsePacket}.
 */
public class InterledgerResponsePacketTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
  public void testInterledgerResponsePacket() {
    InterledgerResponsePacket response = InterledgerResponsePacket.builder().build();
    assertThat(response.typedData()).isEmpty();
    assertThat(response.getData()).isEmpty();
    assertThat(response.toString()).isEqualTo("InterledgerResponsePacket{, data=, typedData=n/a}");
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
  public void handleUnhandledType() {
    expectedException.expect(IllegalStateException.class);
    new InterledgerResponsePacket() {
      @Override
      public byte[] getData() {
        return new byte[0];
      }

      @Override
      public Optional<Object> typedData() {
        return Optional.empty();
      }
    }.handleAndReturn(
      (fulfillPacket) -> fail("Should not be called."),
      (rejectPacket) -> fail("Should not be called.")
    );
  }

  @Test
  public void map() {
    assertThat((Integer) fulfillPacket.map((fulfillPacket) -> 1, (rejectPacket) -> 2)).isEqualTo(1);
    assertThat((Integer) rejectPacket.map((fulfillPacket) -> 1, (rejectPacket) -> 2)).isEqualTo(2);
  }

  @Test
  public void mapWithUnhandledType() {
    expectedException.expect(IllegalStateException.class);
    new InterledgerResponsePacket() {
      @Override
      public byte[] getData() {
        return new byte[0];
      }

      @Override
      public Optional<Object> typedData() {
        return Optional.empty();
      }
    }.map(
      (fulfillPacket) -> fail("Should not be called."),
      (rejectPacket) -> fail("Should not be called.")
    );
  }

  @Test
  public void mapResponse() {
    assertThat((Integer) fulfillPacket.mapResponse($ -> 1)).isEqualTo(1);
    assertThat((Integer) rejectPacket.mapResponse($ -> 2)).isEqualTo(2);
  }

  @Test
  public void withTypeDataOrThisWhenNull() {
    expectedException.expect(NullPointerException.class);
    rejectPacket.withTypedDataOrThis(null);
  }

  @Test
  public void withTypeDataOrThisWhenEmpty() {
    InterledgerResponsePacket packet = rejectPacket.withTypedDataOrThis(Optional.empty());
    assertThat(packet.typedData()).isEmpty();
  }

  @Test
  public void withTypeDataOrThisWhenReject() {
    final Object obj = new Object();
    InterledgerResponsePacket packet = rejectPacket.withTypedDataOrThis(Optional.of(obj));
    assertThat(packet.typedData().get()).isEqualTo(obj);
    assertThat(packet.getData()).isEqualTo(EMPTY_DATA);
  }

  @Test
  public void withTypeDataOrThisWhenFulfill() {
    final Object obj = new Object();
    InterledgerResponsePacket packet = fulfillPacket.withTypedDataOrThis(Optional.of(obj));
    assertThat(packet.typedData().get()).isEqualTo(obj);
    assertThat(packet.getData()).isEqualTo(EMPTY_DATA);
  }

}
