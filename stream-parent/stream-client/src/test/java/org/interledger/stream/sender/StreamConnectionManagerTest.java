package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.stream.StreamConnection;
import org.interledger.stream.StreamConnectionId;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link StreamConnectionManager}.
 */
public class StreamConnectionManagerTest {

  private StreamConnectionManager streamConnectionManager;

  @Before
  public void setUp() {
    this.streamConnectionManager = new StreamConnectionManager();
  }

  @Test
  public void openConnection() {
    StreamConnectionId streamConnectionId = StreamConnectionId.of("foo");
    final StreamConnection streamConnection = streamConnectionManager
        .openConnection(streamConnectionId);
    assertThat(streamConnectionManager.openConnection(streamConnectionId)).isEqualTo(streamConnection);
    assertThat(streamConnectionManager.openConnection(streamConnectionId)).isEqualTo(streamConnection);
  }

  @Test
  public void closeConnection() {
    StreamConnectionId streamConnectionId = StreamConnectionId.of("foo");
    final StreamConnection streamConnection = streamConnectionManager
        .openConnection(streamConnectionId);

    assertThat(streamConnectionManager.closeConnection(streamConnectionId)).get().isEqualTo(streamConnection);
    assertThat(streamConnectionManager.closeConnection(streamConnectionId)).get().isEqualTo(streamConnection);
  }
}
