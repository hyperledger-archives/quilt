package org.interledger.stream.sender;

import static org.mockito.Mockito.mock;

import org.interledger.link.Link;
import org.interledger.stream.crypto.StreamEncryptionService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;

/**
 * Unit tests for {@link SimpleStreamSender}.
 */
@SuppressWarnings("deprecation")
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
    simpleStreamSender = new SimpleStreamSender(linkMock, Duration.ofMillis(10L), streamEncryptionServiceMock);
  }

  @Test
  public void constructWithNullLink() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(null);
  }

  @Test
  public void constructWithNullDuration() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(linkMock, null);
  }

  @Test
  public void constructWithNullEncryptionService() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(linkMock, Duration.ofMillis(10L), null);
  }

  @Test
  public void constructWithNullConnectionManager() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(linkMock, Duration.ofMillis(10L), streamEncryptionServiceMock, null);
  }

  @Test
  public void constructThreeArgWithNullExecutor() {
    expectedException.expect(NullPointerException.class);
    new SimpleStreamSender(
      linkMock, Duration.ofMillis(10L), streamEncryptionServiceMock, mock(StreamConnectionManager.class), null
    );
  }
}
