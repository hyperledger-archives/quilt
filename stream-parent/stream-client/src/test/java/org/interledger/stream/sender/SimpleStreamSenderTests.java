package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.primitives.UnsignedLong;
import org.interledger.link.Link;
import org.interledger.stream.crypto.StreamEncryptionService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;

/**
 * Unit tests for {@link SimpleStreamSenderTests}.
 */
public class SimpleStreamSenderTests {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamEncryptionService streamEncryptionServiceMock;

  @Mock
  private Link linkMock;

  private SimpleStreamSender simpleStreamSender;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    simpleStreamSender = new SimpleStreamSender(streamEncryptionServiceMock, linkMock);
  }

  @Test
  public void constructWithNullEncryptionService() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(null, linkMock);
  }

  @Test
  public void constructWithNullLink() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(streamEncryptionServiceMock, null);
  }

  @Test
  public void constructThreeArgWithNullEncryptionService() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(null, linkMock, mock(ExecutorService.class));
  }

  @Test
  public void constructThreeArgWithNullLink() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(streamEncryptionServiceMock, null, mock(ExecutorService.class));
  }

  @Test
  public void constructThreeArgWithNullExecutor() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(streamEncryptionServiceMock, linkMock, null);
  }

  @Test
  public void connectionStats() {
    SimpleStreamSender.ConnectionStatistics stats = SimpleStreamSender.ConnectionStatistics.builder()
        .numFulfilledPackets(10)
        .numRejectPackets(5)
        .amountDelivered(UnsignedLong.ONE)
        .build();
    assertThat(stats.totalPackets()).isEqualTo(15);
  }
}
