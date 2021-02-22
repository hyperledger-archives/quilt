package org.interledger.stream.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.stream.connection.StreamConnection.StreamConnectionState;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.model.AccountDetails;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unit tests for {@link StreamConnection}.
 */
public class StreamConnectionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private AccountDetails sourceAccountDetailsMock;

  private final InterledgerAddress destinationAddress = InterledgerAddress.of("example.foo");
  private final StreamSharedSecret streamSharedSecret = StreamSharedSecret.of(new byte[32]);

  private StreamConnection streamConnection;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.streamConnection = new StreamConnection(sourceAccountDetailsMock, destinationAddress, streamSharedSecret);
  }

  @Test
  public void testConstructor1WithNullStreamConnectionId() {
    expectedException.expect(NullPointerException.class);
    new StreamConnection(null, destinationAddress, streamSharedSecret);
  }

  @Test
  public void testConstructor1WithNullDestAddress() {
    expectedException.expect(NullPointerException.class);
    new StreamConnection(sourceAccountDetailsMock, null, streamSharedSecret);
  }

  @Test
  public void testConstructor1WithNullSharedSecret() {
    expectedException.expect(NullPointerException.class);
    new StreamConnection(sourceAccountDetailsMock, destinationAddress, null);
  }

  @Test
  public void testConstructor2WithNullStreamConnectionId() {
    expectedException.expect(NullPointerException.class);
    new StreamConnection(null, destinationAddress, streamSharedSecret, Optional.empty());
  }

  @Test
  public void testConstructor2WithNullDestAddress() {
    expectedException.expect(NullPointerException.class);
    new StreamConnection(sourceAccountDetailsMock, null, streamSharedSecret, Optional.empty());
  }

  @Test
  public void testConstructor2WithNullSharedSecret() {
    expectedException.expect(NullPointerException.class);
    new StreamConnection(sourceAccountDetailsMock, destinationAddress, null, Optional.empty());
  }

  @Test
  public void testConstructor2WithNullDenomination() {
    expectedException.expect(NullPointerException.class);
    new StreamConnection(sourceAccountDetailsMock, destinationAddress, streamSharedSecret, null);
  }

  @Test
  public void nextSequence() throws StreamConnectionClosedException {
    assertThat(streamConnection.nextSequence()).isEqualTo(UnsignedLong.ONE);
    assertThat(streamConnection.nextSequence()).isEqualTo(UnsignedLong.valueOf(2L));
    assertThat(streamConnection.nextSequence()).isEqualTo(UnsignedLong.valueOf(3L));
    assertThat(streamConnection.nextSequence()).isEqualTo(UnsignedLong.valueOf(4L));
  }

  @Test
  public void accessors() throws StreamConnectionClosedException {
    assertThat(streamConnection.getCreationDateTime()).isNotNull();
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.AVAILABLE);
    assertThat(streamConnection.getDestinationAddress()).isEqualTo(InterledgerAddress.of("example.foo"));
    assertThat(streamConnection.getDestinationDenomination()).isEmpty();
    assertThat(streamConnection.getStreamSharedSecret()).isNotNull();
    assertThat(streamConnection.getSourceAccountDetails()).isEqualTo(sourceAccountDetailsMock);
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
            return streamConnection.nextSequence();
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
  public void testStreamConnectionClose() {
    assertThat(streamConnection.isClosed()).isFalse();
    try {
      streamConnection.close();
    } catch (IOException e) {
      fail("This error should not have occurred while closing a StreamConnection.");
      e.printStackTrace();
    }
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
    assertThat(streamConnection.getCreationDateTime()).isBefore(DateUtils.now().plusSeconds(1));
  }

  @Test
  public void sequenceIsSafeForSingleSharedSecretWhenStateIsAvailable() {
    assertThat(streamConnection.getConnectionState()).isEqualTo(StreamConnectionState.AVAILABLE);
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.ZERO)).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.ONE)).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(5000L))).isTrue();
    assertThat(streamConnection.sequenceIsSafeForSingleSharedSecret(UnsignedLong.valueOf(Integer.MAX_VALUE))).isTrue();
    assertThat(streamConnection
      .sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_PACKETS_PER_CONNECTION))
      .isTrue();
    assertThat(streamConnection
      .sequenceIsSafeForSingleSharedSecret(
        StreamConnection.MAX_PACKETS_PER_CONNECTION.plus(UnsignedLong.ONE)))
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
    assertThat(streamConnection
      .sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_PACKETS_PER_CONNECTION))
      .isTrue();
    assertThat(streamConnection
      .sequenceIsSafeForSingleSharedSecret(
        StreamConnection.MAX_PACKETS_PER_CONNECTION.plus(UnsignedLong.ONE)))
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
    assertThat(streamConnection
      .sequenceIsSafeForSingleSharedSecret(StreamConnection.MAX_PACKETS_PER_CONNECTION))
      .isFalse();
    assertThat(streamConnection
      .sequenceIsSafeForSingleSharedSecret(
        StreamConnection.MAX_PACKETS_PER_CONNECTION.plus(UnsignedLong.ONE)))
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

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testEquals() {
    assertThat(streamConnection.equals(null)).isFalse();
    StreamConnection identicalStreamConnection = new StreamConnection(
      sourceAccountDetailsMock, destinationAddress, streamSharedSecret
    );
    assertThat(streamConnection.equals(identicalStreamConnection)).isTrue();
    assertThat(identicalStreamConnection.equals(streamConnection)).isTrue();

    StreamConnection nonIdenticalStreamConnection = new StreamConnection(
      sourceAccountDetailsMock, InterledgerAddress.of("example.bar"), streamSharedSecret
    );
    assertThat(streamConnection.equals(nonIdenticalStreamConnection)).isFalse();
    assertThat(nonIdenticalStreamConnection.equals(streamConnection)).isFalse();
  }

  @Test
  public void testHashCode() {
    StreamConnection identicalStreamConnection = new StreamConnection(sourceAccountDetailsMock, destinationAddress,
      streamSharedSecret);
    assertThat(streamConnection.hashCode()).isEqualTo(identicalStreamConnection.hashCode());

    StreamConnection nonIdenticalStreamConnection = new StreamConnection(
      sourceAccountDetailsMock, InterledgerAddress.of("example.bar"), streamSharedSecret
    );
    assertThat(streamConnection.hashCode()).isNotEqualTo(nonIdenticalStreamConnection.hashCode());
  }

  @Test
  public void testToString() {
    final String matchingSerializedRegex = "StreamConnection\\[" +
      "sourceAccountDetails=sourceAccountDetailsMock, " +
      "destinationAddress=InterledgerAddress\\{value=example\\.foo}, " +
      "destinationDenomination=Optional\\.empty, " +
      "creationDateTime=[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.\\d+Z, " +
      "streamConnectionId=StreamConnectionId\\(\\S+\\), " +
      "sequence=1, " +
      "connectionState=AVAILABLE]";

    final String serializedStreamConnection = streamConnection.toString();

    assertThat(serializedStreamConnection.matches(matchingSerializedRegex)).isTrue();
  }
}
