package org.interledger.stream.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.StreamConnectionDetails;
import org.interledger.stream.StreamException;

import org.junit.Before;
import org.junit.Test;

import java.util.Base64;

/**
 * Unit tests for {@link SpspConnectionGenerator}.
 */
public class SpspConnectionGeneratorTest {

  private ServerSecretSupplier serverSecret;

  private SpspConnectionGenerator connectionGenerator;

  @Before
  public void setUp() {
    serverSecret = () -> new byte[32];
    this.connectionGenerator = new SpspConnectionGenerator();
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullSecret() {
    try {
      connectionGenerator.generateConnectionDetails(null, InterledgerAddress.of("example.foo"));
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("serverSecretSupplier must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullReceiverAddress() {
    try {
      connectionGenerator.generateConnectionDetails(() -> new byte[0], null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("receiverAddress must not be null"));
      throw e;
    }
  }

  @Test
  public void generateConnectionDetailsAndDeriveSecret() {
    InterledgerAddress receiverAddress = InterledgerAddress.of("example.receiver");
    StreamConnectionDetails connectionDetails = connectionGenerator
        .generateConnectionDetails(serverSecret, receiverAddress);

    assertThat(connectionDetails.destinationAddress().startsWith(receiverAddress), is(true));
    assertThat(
        connectionDetails.sharedSecret(),
        is(Base64.getUrlEncoder().withoutPadding().encodeToString(
            connectionGenerator.deriveSecretFromAddress(serverSecret, connectionDetails.destinationAddress())
        )));
  }

  @Test(expected = StreamException.class)
  public void assertErrorsIfCannotDeriveAddress() {
    InterledgerAddress receiverAddress = InterledgerAddress.of("example.receiver");
    StreamConnectionDetails connectionDetails = connectionGenerator
        .generateConnectionDetails(serverSecret, receiverAddress);

    receiverAddress = receiverAddress.with("55412631d66b49073b0c36e17a29ba266164fc508bd3eb1c8fc718a02907ce01");

    try {
      connectionGenerator.deriveSecretFromAddress(serverSecret, receiverAddress);
    } catch (StreamException e) {
      assertThat(e.getMessage(),
          is("Invalid Receiver Address (should have been 32 byte long): InterledgerAddress{value=example.receiver.55412631d66b49073b0c36e17a29ba266164fc508bd3eb1c8fc718a02907ce01}"));
      throw e;
    }
  }
}
