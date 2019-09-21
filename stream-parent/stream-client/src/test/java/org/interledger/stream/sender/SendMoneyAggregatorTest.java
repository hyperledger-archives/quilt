package org.interledger.stream.sender;

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

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

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

    this.sendMoneyAggregator = new SendMoneyAggregator(
        Executors.newFixedThreadPool(1), streamCodecContextMock, linkMock, congestionControllerMock,
        streamEncryptionServiceMock, sharedSecret, sourceAddress, destinationAddress, originalAmountToSend
    );
  }

  @Test
  public void sendMoneyWithMaxSequenceMinus1() throws ExecutionException, InterruptedException {
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
}
