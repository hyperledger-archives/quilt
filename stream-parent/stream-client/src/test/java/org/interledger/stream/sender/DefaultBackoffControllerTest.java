package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.Link;
import org.interledger.link.exceptions.LinkRetriesExceededException;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultBackoffControllerTest {

  private final InterledgerAddress destinationAddress = InterledgerAddress.of("test.tryintimes.egg");

  @Test
  public void checkMaxRetries() {
    Link link = mock(Link.class);
    BackoffController backoffController = new DefaultBackoffController(Duration.ofMillis(10), 1.0, 5);
    InterledgerRejectPacket reject = InterledgerRejectPacket.builder()
      .message("can I offer?")
      .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
      .data(new byte[32])
      .build();
    InterledgerPreparePacket prepare = samplePreparePacket();
    when(link.sendPacket(prepare)).thenReturn(reject);
    AtomicInteger numRejects = new AtomicInteger(0);
    try {
      backoffController.sendWithBackoff(link, prepare, numRejects);
      fail("Expected retry exception");
    }
    catch (LinkRetriesExceededException e) {
      assertThat(e.getMessage()).isEqualTo("Max retries of retryable reject exceeded {5}");
    }
    catch (Exception e) {
      fail("Wrong exception type thrown");
    }

    assertThat(numRejects.get()).isEqualTo(5);
    verify(link, times(5)).sendPacket(prepare);
  }

  @Test
  public void checkNoRetriesOnSuccess() {
    Link link = mock(Link.class);
    BackoffController backoffController = new DefaultBackoffController(Duration.ofMillis(10), 1.0, 5);
    InterledgerFulfillPacket fulfill = InterledgerFulfillPacket.builder()
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .data(new byte[32])
      .build();
    InterledgerPreparePacket prepare = samplePreparePacket();
    when(link.sendPacket(prepare)).thenReturn(fulfill);
    AtomicInteger numRejects = new AtomicInteger(0);
    backoffController.sendWithBackoff(link, prepare, numRejects);
    assertThat(numRejects.get()).isZero();
    verify(link, times(1)).sendPacket(prepare);
  }

  @Test
  public void checkNoRetriesOnNonRetryable() {
    Link link = mock(Link.class);
    BackoffController backoffController = new DefaultBackoffController(Duration.ofMillis(10), 1.0, 5);
    InterledgerRejectPacket reject = InterledgerRejectPacket.builder()
      .message("can I offer?")
      .code(InterledgerErrorCode.F02_UNREACHABLE)
      .data(new byte[32])
      .build();
    InterledgerPreparePacket prepare = samplePreparePacket();
    when(link.sendPacket(prepare)).thenReturn(reject);
    AtomicInteger numRejects = new AtomicInteger(0);
    backoffController.sendWithBackoff(link, prepare, numRejects);
    assertThat(numRejects.get()).isEqualTo(1);
    verify(link, times(1)).sendPacket(prepare);
  }

  @Test
  public void checkNoRetriesOnLiquidity() {
    Link link = mock(Link.class);
    BackoffController backoffController = new DefaultBackoffController(Duration.ofMillis(10), 1.0, 5);
    InterledgerRejectPacket reject = InterledgerRejectPacket.builder()
      .message("can I offer?")
      .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
      .data(new byte[32])
      .build();
    InterledgerPreparePacket prepare = samplePreparePacket();
    when(link.sendPacket(prepare)).thenReturn(reject);
    AtomicInteger numRejects = new AtomicInteger(0);
    backoffController.sendWithBackoff(link, prepare, numRejects);
    assertThat(numRejects.get()).isEqualTo(1);
    verify(link, times(1)).sendPacket(prepare);
  }

  private InterledgerPreparePacket samplePreparePacket() {
    return InterledgerPreparePacket.builder()
      .destination(destinationAddress)
      .amount(UnsignedLong.ONE)
      .expiresAt(DateUtils.now())
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .build();
  }
}
