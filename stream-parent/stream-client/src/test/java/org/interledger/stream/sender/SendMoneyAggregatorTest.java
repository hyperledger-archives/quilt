package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.StreamPacket.MAX_FRAMES_PER_CONNECTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.sender.SimpleStreamSender.SendMoneyAggregator;

import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link SendMoneyAggregator}.
 */
public class SendMoneyAggregatorTest {

  @Mock
  private CodecContext streamCodecContextMock;
  @Mock
  private Link linkMock;
  @Mock
  private CongestionController congestionControllerMock;
  @Mock
  private StreamEncryptionService streamEncryptionServiceMock;

  private byte[] sharedSecret = new byte[32];
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

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    this.sendMoneyAggregator = new SendMoneyAggregator(
        executor, Maps.newConcurrentMap(), streamCodecContextMock, linkMock, congestionControllerMock,
        streamEncryptionServiceMock, sharedSecret, sourceAddress, destinationAddress, originalAmountToSend,
        Duration.ofSeconds(60)
    );
  }

  @Test
  public void sendMoneyWithMaxSequenceMinus1() throws ExecutionException, InterruptedException {
    InterledgerRejectPacket reject = mock(InterledgerRejectPacket.class);
    AtomicBoolean calledSend = new AtomicBoolean(false);
    // don't mess with these because if you do your test wil have nondeterministic results
    // you need to make sure to return true from hasInFlight ONLY WHEN WE HAVE CALLED sendPacket
    // if you always return true, the test will run indefinitely
    // if you always return false, the test will fail nondeterministically
    // you may not like .thenAnswer, but it's necessary here
    when(linkMock.sendPacket(any())).thenAnswer((Answer<InterledgerRejectPacket>) invocationOnMock -> {
      calledSend.set(true);
      return reject;
    });
    when(congestionControllerMock.hasInFlight()).thenAnswer((Answer<Boolean>) invocationOnMock -> !calledSend.get());

    sendMoneyAggregator.setSequenceForTesting(MAX_FRAMES_PER_CONNECTION.minus(UnsignedLong.ONE));
    sendMoneyAggregator.send().get();

    // Expect 1 Link call for the OpenConnection, and 1 Link call for the CloseConnection
    Mockito.verify(linkMock, times(2)).sendPacket(any());
  }

  @Test
  public void sendMoneyWithMaxSequence() throws ExecutionException, InterruptedException {
    sendMoneyAggregator.setSequenceForTesting(MAX_FRAMES_PER_CONNECTION);
    sendMoneyAggregator.send().get();

    // Expect 1 Link call for the CloseConnection
    Mockito.verify(linkMock).sendPacket(any());
  }

  @Test
  public void sendMoneyWithMaxSequencePlus1() throws ExecutionException, InterruptedException {
    sendMoneyAggregator.setSequenceForTesting(MAX_FRAMES_PER_CONNECTION.plus(UnsignedLong.ONE));
    sendMoneyAggregator.send().get();

    // Expect 1 Link call for the CloseConnection
    Mockito.verify(linkMock).sendPacket(any());
  }

  @Test
  public void soldierOn() {
    // if money in flight, always soldier on
    // else
    //   you haven't reached max packets
    //   and you haven't delivered the full amount
    //   and you haven't timed out

    // always true when money in flight
    when(congestionControllerMock.hasInFlight()).thenReturn(true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();
    sendMoneyAggregator.setSequenceForTesting(UnsignedLong.MAX_VALUE);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();
    sendMoneyAggregator.setSequenceForTesting(UnsignedLong.ONE);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    when(congestionControllerMock.hasInFlight()).thenReturn(false);

    // max packets
    sendMoneyAggregator.setSequenceForTesting(UnsignedLong.MAX_VALUE);
    assertThat(sendMoneyAggregator.soldierOn(false)).isFalse();
    sendMoneyAggregator.setSequenceForTesting(UnsignedLong.ONE);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    // delivered amount
    sendMoneyAggregator.setDeliveredAmountForTesting(UnsignedLong.valueOf(10l));
    assertThat(sendMoneyAggregator.soldierOn(false)).isFalse();
    sendMoneyAggregator.setDeliveredAmountForTesting(UnsignedLong.valueOf(9l));
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    // timed out
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

  }

  @Test
  public void canBeScheduled() {
    assertThat(sendMoneyAggregator.canBeScheduled(true, UnsignedLong.ONE)).isFalse();
    assertThat(sendMoneyAggregator.canBeScheduled(true, UnsignedLong.MAX_VALUE)).isFalse();
    assertThat(sendMoneyAggregator.canBeScheduled(false, UnsignedLong.MAX_VALUE)).isFalse();
    assertThat(sendMoneyAggregator.canBeScheduled(false, UnsignedLong.ONE)).isTrue();
  }
}
