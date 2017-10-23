package org.interledger.ilp;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;

import org.interledger.InterledgerAddress;
import org.interledger.ilp.InterledgerProtocolError.Builder;
import org.interledger.ilp.InterledgerProtocolError.ErrorCode;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

/**
 * Unit tests for {@link InterledgerProtocolError} and {@link Builder}.
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
      new Builder().build();
      fail("Builder should have thrown an exception but did not!");
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("errorCode must not be null!"));
    }

    try {
      new Builder().errorCode(ErrorCode.T00_INTERNAL_ERROR).build();
      fail("Builder should have thrown an exception but did not!");
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("triggeredByAddress must not be null!"));
    }

    try {
      final InterledgerProtocolError error
          = new Builder().errorCode(ErrorCode.T00_INTERNAL_ERROR).triggeredByAddress(FOO).build();
      assertThat(error.getTriggeredAt(), is(not(nullValue())));
    } catch (Exception e) {
      fail("Builder threw an exception but should not have!");
    }

  }

  @Test
  public void testBuilderWithNullValues() throws Exception {

    final Builder builder = new Builder();

    try {
      builder.errorCode(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("errorCode must not be null!"));
    }

    try {
      builder.triggeredByAddress(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("triggeredByAddress must not be null!"));
    }

    try {
      builder.forwardedByAddresses(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("forwardedByAddresses must not be null!"));
    }

    try {
      builder.addForwardedByAddress(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("forwardedByAddress must not be null!"));
    }

    try {
      builder.triggeredAt(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("triggeredAt must not be null!"));
    }

    try {
      builder.data(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("data must not be null!"));
    }
  }

  @Test
  public void testEqualsHashCode() throws Exception {

    final Instant now = Instant.now();
    final InterledgerProtocolError interledgerProtocolError1 = new Builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(now)
        .build();

    final InterledgerProtocolError interledgerProtocolError2 = new Builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(now)
        .build();

    assertThat(interledgerProtocolError1, is(interledgerProtocolError2));
    assertThat(interledgerProtocolError2, is(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.equals(interledgerProtocolError2));
    assertTrue(interledgerProtocolError2.equals(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.hashCode() == interledgerProtocolError2.hashCode());

    final InterledgerProtocolError interledgerProtocolErrorOther = new Builder()
        .errorCode(ErrorCode.T99_APPLICATION_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(Instant.now())
        .build();

    assertFalse(interledgerProtocolError1.equals(interledgerProtocolErrorOther));
    assertFalse(interledgerProtocolErrorOther.equals(interledgerProtocolError1));
    assertFalse(interledgerProtocolError1.hashCode() == interledgerProtocolErrorOther.hashCode());
  }

  @Test
  public void testCopyBuilder() throws Exception {
    final InterledgerProtocolError interledgerProtocolError1 = new Builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(Instant.now())
        .build();

    final InterledgerProtocolError interledgerProtocolError2 = new Builder(
        interledgerProtocolError1).build();

    assertTrue(interledgerProtocolError1.equals(interledgerProtocolError2));
    assertTrue(interledgerProtocolError2.equals(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.hashCode() == interledgerProtocolError2.hashCode());

    final InterledgerProtocolError interledgerProtocolErrorOther = new Builder()
        .errorCode(ErrorCode.T99_APPLICATION_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of())
        .triggeredAt(Instant.now())
        .build();

    assertFalse(interledgerProtocolError1.equals(interledgerProtocolErrorOther));
    assertFalse(interledgerProtocolErrorOther.equals(interledgerProtocolError1));
    assertFalse(interledgerProtocolError1.hashCode() == interledgerProtocolErrorOther.hashCode());
  }

  @Test
  public void testAddForwardedByAddress() throws Exception {

    final InterledgerProtocolError interledgerProtocolError1 = new Builder()
        .errorCode(ErrorCode.T00_INTERNAL_ERROR)
        .triggeredByAddress(FOO)
        .forwardedByAddresses(ImmutableList.of(BAR))
        .triggeredAt(Instant.now())
        .build();

    final InterledgerProtocolError interledgerProtocolError2 = new Builder(
        interledgerProtocolError1)
        .addForwardedByAddress(BAZ)
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
      new Builder()
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
