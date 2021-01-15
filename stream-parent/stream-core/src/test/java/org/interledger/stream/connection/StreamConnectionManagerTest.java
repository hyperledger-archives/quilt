package org.interledger.stream.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.StreamConnectionId;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.model.AccountDetails;

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
  public void openAndCloseConnection() {

    final AccountDetails sourceAccountDetails = mock(AccountDetails.class);
    final InterledgerAddress destinationAddress = InterledgerAddress.of("example.foo");
    final StreamSharedSecret streamSharedSecret = StreamSharedSecret.of(new byte[32]);

    StreamConnectionId streamConnectionId = StreamConnectionId.from(destinationAddress, streamSharedSecret);
    final StreamConnection streamConnection = streamConnectionManager
      .openConnection(
        sourceAccountDetails,
        destinationAddress,
        streamSharedSecret
      );
    assertThat(streamConnectionManager
      .openConnection(
        sourceAccountDetails,
        destinationAddress,
        streamSharedSecret
      )).isEqualTo(streamConnection);
    assertThat(streamConnectionManager
      .openConnection(
        sourceAccountDetails,
        destinationAddress,
        streamSharedSecret
      )).isEqualTo(streamConnection);

    assertThat(streamConnectionManager.closeConnection(streamConnectionId)).get().isEqualTo(streamConnection);
    assertThat(streamConnectionManager.closeConnection(streamConnectionId)).get().isEqualTo(streamConnection);
  }
}
