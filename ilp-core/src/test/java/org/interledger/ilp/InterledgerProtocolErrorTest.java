package org.interledger.ilp;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;

import org.interledger.InterledgerAddress;
import org.interledger.ilp.InterledgerProtocolError.ErrorCode;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

/**
 * Unit tests for {@link InterledgerProtocolError} and {@link InterledgerProtocolErrorBuilder}.
 */
public class InterledgerProtocolErrorTest {

  private static final InterledgerAddress FOO = InterledgerAddress.of("test1.foo");
  private static final InterledgerAddress BAR = InterledgerAddress.of("test1.bar");
  private static final InterledgerAddress BAZ = InterledgerAddress.of("test1.baz");

  @Test
  public void testBuild() throws Exception {
    final ErrorCode errorCode = ErrorCode.T00_INTERNAL_ERROR;
    final InterledgerAddress triggeredBy = FOO;
    final List<InterledgerAddress> forwardedByAddresses = ImmutableList.of(BAR);
    final Instant triggeredAt = Instant.now();
    final byte[] data = new byte[]{127};

    final InterledgerProtocolError interledgerProtocolError =
        InterledgerProtocolError.builder()
            .errorCode(errorCode)
            .triggeredByAddress(triggeredBy)
            .forwardedByAddresses(forwardedByAddresses)
            .triggeredAt(triggeredAt)
            .data(data)
            .build();

    assertThat(interledgerProtocolError.getErrorCode(), is(errorCode));
    assertThat(interledgerProtocolError.getTriggeredByAddress(), is(triggeredBy));
    assertThat(interledgerProtocolError.getForwardedByAddresses(), is(forwardedByAddresses));
    assertThat(interledgerProtocolError.getTriggeredAt(), is(triggeredAt));
    assertThat(interledgerProtocolError.getData().get(), is(data));
  }

  @Test
  public void testBuildWithoutOptionalData() throws Exception {
    final ErrorCode errorCode = ErrorCode.T00_INTERNAL_ERROR;
    final InterledgerAddress triggeredBy = FOO;
    final Instant triggeredAt = Instant.now();

    final InterledgerProtocolError interledgerProtocolError =
        InterledgerProtocolError.builder()
            .errorCode(errorCode)
            .triggeredByAddress(triggeredBy)
            .triggeredAt(triggeredAt)
            .build();

    assertThat(interledgerProtocolError.getErrorCode(), is(errorCode));
    assertThat(interledgerProtocolError.getTriggeredByAddress(), is(triggeredBy));
    assertThat(interledgerProtocolError.getForwardedByAddresses().size(), is(0));
    assertThat(interledgerProtocolError.getTriggeredAt(), is(triggeredAt));
    assertThat(interledgerProtocolError.getData().isPresent(), is(false));
  }

  @Test
  public void testBuildWithUnintializedValues() throws Exception {
    try {
      InterledgerProtocolError.builder().build();
      fail("Builder should have thrown an exception but did not!");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalStateException);
      assertTrue(e.getMessage().startsWith("Cannot build InterledgerProtocolError, "
          + "some of required attributes are not set"));
    }

    try {
      InterledgerProtocolError.builder().errorCode(ErrorCode.T00_INTERNAL_ERROR).build();
      fail("Builder should have thrown an exception but did not!");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalStateException);
      assertTrue(e.getMessage().startsWith("Cannot build InterledgerProtocolError, "
          + "some of required attributes are not set"));
    }

    final InterledgerProtocolError error = InterledgerProtocolError.builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .triggeredAt(Instant.now())
        .build();
    assertThat(error.getTriggeredAt(), is(not(nullValue())));

  }

  @Test
  public void testBuilderWithNullValues() throws Exception {

    final InterledgerProtocolErrorBuilder builder = InterledgerProtocolError.builder();

    try {
      builder.errorCode(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("errorCode"));
    }

    try {
      builder.triggeredByAddress(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("triggeredByAddress"));
    }

    try {
      builder.forwardedByAddresses(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      //TODO Error in Immutables generated code. No explicit null check
      // assertThat(e.getMessage(), is("forwardedByAddresses"));
    }

    try {
      builder.addForwardedByAddresses((InterledgerAddress) null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("forwardedByAddresses element"));
    }

    try {
      builder.triggeredAt(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("triggeredAt"));
    }

    try {
      builder.data((byte[]) null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("data"));
    }
  }

  @Test
  public void testEqualsHashCode() throws Exception {

    final Instant now = Instant.now();
    final InterledgerProtocolError interledgerProtocolError1
        = InterledgerProtocolError.builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(now)
        .build();

    final InterledgerProtocolError interledgerProtocolError2
        = InterledgerProtocolError.builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(now)
        .build();

    assertThat(interledgerProtocolError1, is(interledgerProtocolError2));
    assertThat(interledgerProtocolError2, is(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.equals(interledgerProtocolError2));
    assertTrue(interledgerProtocolError2.equals(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.hashCode()
        == interledgerProtocolError2.hashCode());

    final InterledgerProtocolError interledgerProtocolErrorOther
        = InterledgerProtocolError.builder()
        .errorCode(ErrorCode.T99_APPLICATION_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(Instant.now())
        .build();

    assertFalse(interledgerProtocolError1.equals(interledgerProtocolErrorOther));
    assertFalse(interledgerProtocolErrorOther.equals(interledgerProtocolError1));
    assertFalse(interledgerProtocolError1.hashCode()
        == interledgerProtocolErrorOther.hashCode());
  }

  @Test
  public void testCopyBuilder() throws Exception {
    final InterledgerProtocolError interledgerProtocolError1
        = InterledgerProtocolError.builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(Instant.now())
        .build();

    final InterledgerProtocolError interledgerProtocolError2
        = InterledgerProtocolError.builder().from(
        interledgerProtocolError1).build();

    assertTrue(interledgerProtocolError1.equals(interledgerProtocolError2));
    assertTrue(interledgerProtocolError2.equals(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.hashCode()
        == interledgerProtocolError2.hashCode());

    final InterledgerProtocolError interledgerProtocolErrorOther
        = InterledgerProtocolError.builder()
        .errorCode(ErrorCode.T99_APPLICATION_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(Instant.now())
        .build();

    assertFalse(interledgerProtocolError1.equals(interledgerProtocolErrorOther));
    assertFalse(interledgerProtocolErrorOther.equals(interledgerProtocolError1));
    assertFalse(interledgerProtocolError1.hashCode()
        == interledgerProtocolErrorOther.hashCode());
  }

  @Test
  public void testAddForwardedByAddress() throws Exception {

    final InterledgerProtocolError interledgerProtocolError1
        = InterledgerProtocolError.builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of(BAR))
        .triggeredAt(Instant.now())
        .build();

    final InterledgerProtocolError interledgerProtocolError2 = InterledgerProtocolError
        .builder().from(interledgerProtocolError1)
        .addForwardedByAddresses(BAZ)
        .build();

    assertThat(interledgerProtocolError1.equals(interledgerProtocolError2), is(false));

    assertThat(interledgerProtocolError1.getForwardedByAddresses().contains(FOO), is(false));
    assertThat(interledgerProtocolError1.getForwardedByAddresses().contains(BAR), is(true));
    assertThat(interledgerProtocolError1.getForwardedByAddresses().contains(BAZ), is(false));

    assertThat(interledgerProtocolError2.getForwardedByAddresses().contains(FOO), is(false));
    assertThat(interledgerProtocolError2.getForwardedByAddresses().contains(BAR), is(true));
    assertThat(interledgerProtocolError2.getForwardedByAddresses().contains(BAZ), is(true));
  }

  @Test
  public void testTriggeredByNotInForwardedByAddress() throws Exception {
    try {
      InterledgerProtocolError.builder()
          .errorCode(ErrorCode.T00_INTERNAL_ERROR)
          .triggeredByAddress(FOO)
          .forwardedByAddresses(ImmutableList.of(FOO, BAR))
          .triggeredAt(Instant.now())
          .build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(
          "TriggeredByAddress \"test1.foo\" was found in the ForwardedByAddresses list, which"
              + " indicates an Interledger packet loop!"));
    }
  }
}
