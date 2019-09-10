package org.interledger.stream.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.StreamConnectionDetails;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link StatelessStreamServer}.
 */
public class StatelessStreamServerTest {

  private StreamServer streamServer;

  @Before
  public void setUp() {
    ServerSecretSupplier serverSecret = () -> new byte[32];
    streamServer = new StatelessStreamServer(serverSecret);
  }

  @Test
  public void setupStream() {
    InterledgerAddress receiverAddress = InterledgerAddress.of("example.receiver");
    StreamConnectionDetails connectionDetails = streamServer.setupStream(receiverAddress);

    assertThat(connectionDetails.destinationAddress().startsWith(receiverAddress), is(true));
    assertThat(connectionDetails.sharedSecret(), is(123));

  }
}
