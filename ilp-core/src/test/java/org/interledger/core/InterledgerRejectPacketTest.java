package org.interledger.core;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

/**
 * Unit tests for {@link InterledgerRejectPacket} and {@link InterledgerRejectPacketBuilder}.
 */
public class InterledgerRejectPacketTest {

  private static final InterledgerAddress FOO = InterledgerAddress.of("test1.foo.foo");
  private static final InterledgerAddress BAR = InterledgerAddress.of("test1.bar.bar");
  private static final InterledgerAddress BAZ = InterledgerAddress.of("test1.baz.baz");

  @Test
  public void testBuild() throws Exception {
    final InterledgerErrorCode errorCode = InterledgerErrorCode.T00_INTERNAL_ERROR;
    final String message = "Test Error";
    final InterledgerAddress triggeredBy = FOO;
    final List<InterledgerAddress> forwardedByAddresses = ImmutableList.of(BAR);
    final Instant triggeredAt = Instant.now();
    final byte[] data = new byte[] {127};

    final InterledgerRejectPacket interledgerProtocolError =
        InterledgerRejectPacket.builder()
            .code(errorCode)
            .message(message)
            .triggeredBy(triggeredBy)
            .data(data)
            .build();

    assertThat(interledgerProtocolError.getCode(), is(errorCode));
    assertThat(interledgerProtocolError.getTriggeredBy(), is(triggeredBy));
    assertThat(interledgerProtocolError.getData(), is(data));
  }

  //  @Test
  //  public void testBuildWithoutOptionalData() throws Exception {
  //    final InterledgerErrorCode errorCode = InterledgerErrorCode.T00_INTERNAL_ERROR;
  //    final InterledgerAddress triggeredBy = FOO;
  //
  //    final InterledgerRejectPacket interledgerProtocolError =
  //        InterledgerRejectPacket.builder()
  //            .code(errorCode)
  //            .triggeredBy(triggeredBy)
  //            .build();
  //
  //    assertThat(interledgerProtocolError.getCode(), is(errorCode));
  //    assertThat(interledgerProtocolError.getTriggeredBy(), is(triggeredBy));
  //    assertThat(interledgerProtocolError.getData(), is(nullValue()));
  //  }

  @Test
  public void testBuildWithUnintializedValues() throws Exception {
    try {
      InterledgerRejectPacket.builder().build();
      fail("Builder should have thrown an exception but did not!");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalStateException);
      assertTrue(e.getMessage().startsWith("Cannot build InterledgerRejectPacket, "
          + "some of required attributes are not set"));
    }

    try {
      InterledgerRejectPacket.builder().code(InterledgerErrorCode.T00_INTERNAL_ERROR).build();
      fail("Builder should have thrown an exception but did not!");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalStateException);
      assertTrue(e.getMessage().startsWith("Cannot build InterledgerRejectPacket, "
          + "some of required attributes are not set"));
    }

  }

  @Test
  public void testBuilderWithNullValues() throws Exception {

    final InterledgerRejectPacketBuilder builder = InterledgerRejectPacket.builder();

    try {
      builder.code(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("code"));
    }

    try {
      builder.triggeredBy(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("triggeredBy"));
    }

    try {
      builder.data((byte[]) null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

  @Test
  public void testEqualsHashCode() throws Exception {

    final Instant now = Instant.now();
    final String message = "Test Message";
    final InterledgerRejectPacket interledgerProtocolError1
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message(message)
        .triggeredBy(FOO)
        .data(new byte[]{})
        .build();

    final InterledgerRejectPacket interledgerProtocolError2
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message(message)
        .triggeredBy(FOO)
        .data(new byte[]{})
        .build();

    assertThat(interledgerProtocolError1, is(interledgerProtocolError2));
    assertThat(interledgerProtocolError2, is(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.equals(interledgerProtocolError2));
    assertTrue(interledgerProtocolError2.equals(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.hashCode()
        == interledgerProtocolError2.hashCode());

    final InterledgerRejectPacket interledgerProtocolErrorOther
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T99_APPLICATION_ERROR)
        .message(message)
        .triggeredBy(FOO)
        .data(new byte[]{})
        .build();

    assertFalse(interledgerProtocolError1.equals(interledgerProtocolErrorOther));
    assertFalse(interledgerProtocolErrorOther.equals(interledgerProtocolError1));
    assertFalse(interledgerProtocolError1.hashCode()
        == interledgerProtocolErrorOther.hashCode());
  }

  @Test
  public void testCopyBuilder() throws Exception {
    final InterledgerRejectPacket interledgerProtocolError1
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message("TEST")
        .triggeredBy(FOO)
        .data(new byte[32])
        .build();

    final InterledgerRejectPacket interledgerProtocolError2
        = InterledgerRejectPacket.builder().from(
        interledgerProtocolError1).build();

    assertTrue(interledgerProtocolError1.equals(interledgerProtocolError2));
    assertTrue(interledgerProtocolError2.equals(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.hashCode()
        == interledgerProtocolError2.hashCode());

    final InterledgerRejectPacket interledgerProtocolErrorOther
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T99_APPLICATION_ERROR)
        .message("TEST")
        .triggeredBy(FOO)
        .data(new byte[32])
        .build();

    assertFalse(interledgerProtocolError1.equals(interledgerProtocolErrorOther));
    assertFalse(interledgerProtocolErrorOther.equals(interledgerProtocolError1));
    assertFalse(interledgerProtocolError1.hashCode()
        == interledgerProtocolErrorOther.hashCode());
  }

}
