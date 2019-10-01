package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.SharedSecret;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;
import org.interledger.stream.StreamConnection;
import org.interledger.stream.StreamConnectionClosedException;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.sender.SimpleStreamSender.SendMoneyAggregator;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Unit tests for {@link SendMoneyAggregator}.
 */
public class SendMoneyAggregatorTest {

  // 5 seconds max per method tested
  @Rule
  public Timeout globalTimeout = Timeout.seconds(5);

  @Mock
  private CodecContext streamCodecContextMock;
  @Mock
  private Link linkMock;
  @Mock
  private CongestionController congestionControllerMock;
  @Mock
  private StreamEncryptionService streamEncryptionServiceMock;
  @Mock
  private StreamConnection streamConnectionMock;

  private SharedSecret sharedSecret = SharedSecret.of(new byte[32]);
  private InterledgerAddress sourceAddress = InterledgerAddress.of("example.source");
  private InterledgerAddress destinationAddress = InterledgerAddress.of("example.destination");
  private UnsignedLong originalAmountToSend = UnsignedLong.valueOf(10L);

  private SendMoneyAggregator sendMoneyAggregator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(congestionControllerMock.getMaxAmount()).thenReturn(UnsignedLong.ONE);
    when(streamEncryptionServiceMock.encrypt(any(), any())).thenReturn(new byte[32]);
    when(streamEncryptionServiceMock.decrypt(any(), any())).thenReturn(new byte[32]);
    when(linkMock.sendPacket(any())).thenReturn(mock(InterledgerRejectPacket.class));

    final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    this.sendMoneyAggregator = new SendMoneyAggregator(
        executor, streamConnectionMock, streamCodecContextMock, linkMock, congestionControllerMock,
        streamEncryptionServiceMock, sharedSecret, sourceAddress, destinationAddress, originalAmountToSend,
        Optional.of(Duration.ofSeconds(60))
    );
  }

  @Test
  public void sendMoneyWhenTimedOut()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false, true);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();

    // Expect 0 Link calls since close Connection is not called automatically
    Mockito.verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void sendMoneyWhenMoreToSendButTimedOut()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false, true);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();

    // Expect 0 Link calls since close Connection is not called automatically
    Mockito.verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void sendMoneyWhenNoMoreToSend()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false, false);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();

    // Expect 0 Link calls since close Connection is not called automatically
    Mockito.verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void sendMoneyWhenConnectionIsClosed()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, true, true, false);
    when(streamConnectionMock.nextSequence())
        .thenReturn(StreamConnection.MAX_FRAMES_PER_CONNECTION.plus(UnsignedLong.ONE));

    sendMoneyAggregator.send().get();

    // Expect 0 Link calls since close Connection is not called automatically
    Mockito.verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void soldierOn() {
    // if money in flight, always soldier on
    // else
    //   you haven't reached max packets
    //   and you haven't delivered the full amount
    //   and you haven't timed out

    setSoldierOnBooleans(false, false, false, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isFalse();

    setSoldierOnBooleans(false, false, false, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

    setSoldierOnBooleans(false, false, true, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    setSoldierOnBooleans(false, false, true, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

    setSoldierOnBooleans(false, true, false, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isFalse();

    setSoldierOnBooleans(false, true, false, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

    setSoldierOnBooleans(false, true, true, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isFalse();

    setSoldierOnBooleans(false, true, true, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

    setSoldierOnBooleans(true, false, false, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    setSoldierOnBooleans(true, false, false, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, false, true, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    setSoldierOnBooleans(true, false, true, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, true, false, false);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, true, false, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, true, true, false);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, true, true, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();
  }

  /**
   * Helper method to set the soldierOn mock values for clearer test coverage.
   */
  private void setSoldierOnBooleans(
      final boolean moneyInFlight, final boolean streamConnectionClosed, final boolean moreToSend,
      final boolean timeoutReached
  ) {
    when(congestionControllerMock.hasInFlight()).thenReturn(moneyInFlight);
    when(streamConnectionMock.isClosed()).thenReturn(streamConnectionClosed);
    if (moreToSend) {
      sendMoneyAggregator.setDeliveredAmountForTesting(UnsignedLong.ZERO);
    } else {
      sendMoneyAggregator.setDeliveredAmountForTesting(UnsignedLong.valueOf(10L));
    }
  }
}
