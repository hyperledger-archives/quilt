package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.stream.StreamConnection.StreamConnectionState;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unit tests for {@link StreamConnection}.
 */
public class StreamConnectionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private StreamConnection streamConnection;

  @Before
  public void setUp() {
    this.streamConnection = new StreamConnection(StreamConnectionId.of("foo"));
  }

  @Test
  public void testConstructorWithNullStreamConnectionId() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("streamConnectionId must not be null");
    new StreamConnection(null);
  }

  @Test
  public void testConstructorWithNullAddress() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("receiverAddress must not be null");
    new StreamConnection(null, SharedSecret.of(new byte[32]));
  }

  @Test
  public void testConstructorWithNullSharedSecret() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("sharedSecret must not be null");
    new StreamConnection(mock(InterledgerAddress.class), null);
  }

  @Test
  public void testConstructorWithAddressAndSecret() {
    streamConnection = new StreamConnection(InterledgerAddress.of("example.foo"), SharedSecret.of(new byte[32]));
    assertThat(streamConnection.getStreamConnectionId())
        .isEqualTo(StreamConnectionId.of("246307596a10c1ba057f56cd6d588ed0d11cf3f8817c937265e93950af53751f"));
  }

  @Test
  public void testStreamConnectionId() {
    StreamConnectionId streamConnectionId = StreamConnectionId.of("foo");
    assertThat(new StreamConnection(streamConnectionId).getStreamConnectionId()).isEqualTo(streamConnectionId);
  }

  @Test
  public void nextSequence() throws StreamConnectionClosedException {
    assertThat(streamConnection.nextSequence()).isEqualTo(UnsignedLong.ONE);
    assertThat(streamConnection.nextSequence()).isEqualTo(UnsignedLong.valueOf(2L));
    assertThat(streamConnection.nextSequence()).isEqualTo(UnsignedLong.valueOf(3L));
    assertThat(streamConnection.nextSequence()).isEqualTo(UnsignedLong.valueOf(4L));
  }

  @Test
  public void nextSequenceMultiThreaded() throws StreamConnectionClosedException {
    final List<CompletableFuture<UnsignedLong>> allFutures = Lists.newArrayList();
    final int numRepetitions = 50000;

    final ExecutorService executorService = Executors.newFixedThreadPool(10);

    for (int i = 0; i < numRepetitions; i++) {
      allFutures.add(
          CompletableFuture.supplyAsync(() -> {
            try {
              UnsignedLong sequence = streamConnection.nextSequence();
              return sequence;
            } catch (StreamConnectionClosedException e) {
              throw new RuntimeException(e);
            }
          }, executorService)
      );
    }

    CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
    assertThat(streamConnection.nextSequence().longValue()).isEqualTo(numRepetitions + 1);
  }

  @Test
  public void nextSequenceWhenClosed() throws StreamConnectionClosedException {
    expectedException.expect(StreamConnectionClosedException.class);
    streamConnection.closeConnection();
    streamConnection.nextSequence();
  }

  @Test
  public void transitionConnectionState() {
    assertThat(streamConnection.isClosed()).isFalse();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.AVAILABLE);
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.AVAILABLE);
    streamConnection.transitionConnectionState();
    assertThat(streamConnection.isClosed()).isFalse();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.OPEN);
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.OPEN);
    streamConnection.transitionConnectionState();
    assertThat(streamConnection.isClosed()).isTrue();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.CLOSED);
    assertThat(streamConnection.isClosed()).isTrue();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.CLOSED);
    streamConnection.transitionConnectionState();
    streamConnection.transitionConnectionState();
    streamConnection.transitionConnectionState();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.CLOSED);
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.CLOSED);
    assertThat(streamConnection.isClosed()).isTrue();
  }

  @Test
  public void transitionConnectionStateAfterConnectionClosed() {
    streamConnection.closeConnection();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.CLOSED);
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.CLOSED);
    streamConnection.transitionConnectionState();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.CLOSED);
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.CLOSED);
  }

  @Test
  public void getCreationDateTime() {
    assertThat(streamConnection.getCreationDateTime()).isNotNull();
    assertThat(streamConnection.getCreationDateTime()).isBefore(Instant.now().plusSeconds(1));
  }

  @Test
  public void sequenceIsSafeForSingleSharedSecretWhenStateIsAvailable() {
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.AVAILABLE);
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.ZERO)).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.ONE)).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(5000L))).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE))).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_FRAMES_PER_CONNECTION))
        .isTrue();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_FRAMES_PER_CONNECTION.plus(UnsignedLong.ONE)))
        .isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE))).isTrue();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE).plus(UnsignedLong.ONE)))
        .isTrue();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE).plus(UnsignedLong.valueOf(2L))))
        .isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Long.MAX_VALUE))).isFalse();
  }

  @Test
  public void sequenceIsSafeForSingleSharedSecretWhenStateIsOpen() {
    streamConnection.transitionConnectionState();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.OPEN);
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.ZERO)).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.ONE)).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(5000L))).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE))).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_FRAMES_PER_CONNECTION))
        .isTrue();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_FRAMES_PER_CONNECTION.plus(UnsignedLong.ONE)))
        .isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE))).isTrue();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE).plus(UnsignedLong.ONE)))
        .isTrue();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE).plus(UnsignedLong.valueOf(2L))))
        .isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Long.MAX_VALUE))).isFalse();
  }

  @Test
  public void sequenceIsSafeWhenClosed() {
    streamConnection.closeConnection();

    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.ZERO)).isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.ONE)).isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(5000L))).isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE))).isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_FRAMES_PER_CONNECTION))
        .isFalse();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_FRAMES_PER_CONNECTION.plus(UnsignedLong.ONE)))
        .isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE))).isFalse();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE).plus(UnsignedLong.ONE)))
        .isFalse();
    assertThat(streamConnection
        .sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE).plus(UnsignedLong.valueOf(2L))))
        .isFalse();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Long.MAX_VALUE))).isFalse();
  }

  @Test
  public void testEquals() {
    assertThat(streamConnection.equals(null)).isFalse();
    assertThat(streamConnection.equals(streamConnection)).isTrue();
    StreamConnection identicalStreamConnection = new StreamConnection(StreamConnectionId.of("foo"));
    assertThat(streamConnection.equals(identicalStreamConnection)).isTrue();
    assertThat(identicalStreamConnection.equals(streamConnection)).isTrue();

    StreamConnection nonIdenticalStreamConnection = new StreamConnection(StreamConnectionId.of("foo1"));
    assertThat(streamConnection.equals(nonIdenticalStreamConnection)).isFalse();
    assertThat(nonIdenticalStreamConnection.equals(streamConnection)).isFalse();
  }

  @Test
  public void testHashCode() {
    StreamConnection identicalStreamConnection = new StreamConnection(StreamConnectionId.of("foo"));
    assertThat(streamConnection.hashCode()).isEqualTo(identicalStreamConnection.hashCode());

    StreamConnection nonIdenticalStreamConnection = new StreamConnection(StreamConnectionId.of("foo1"));
    assertThat(streamConnection.hashCode()).isNotEqualTo(nonIdenticalStreamConnection.hashCode());
  }
}
