package org.interledger.stream.receiver;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.StreamException;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SpspStreamConnectionGenerator}.
 */
public class SpspStreamConnectionGeneratorTest {

  private ServerSecretSupplier serverSecret;

  private SpspStreamConnectionGenerator connectionGenerator;

  @Before
  public void setUp() {
    serverSecret = () -> new byte[32];
    this.connectionGenerator = new SpspStreamConnectionGenerator();
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullSecret() {
    try {
      connectionGenerator.generateConnectionDetails(null, InterledgerAddress.of("example.foo"));
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("serverSecretSupplier must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullReceiverAddress() {
    try {
      connectionGenerator.generateConnectionDetails(() -> new byte[0], null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("receiverAddress must not be null");
      throw e;
    }
  }

  @Test
  public void generateConnectionDetailsAndDeriveSecret() {
    InterledgerAddress receiverAddress = InterledgerAddress.of("example.receiver");
    StreamConnectionDetails connectionDetails = connectionGenerator
        .generateConnectionDetails(serverSecret, receiverAddress);

    assertThat(connectionDetails.destinationAddress().startsWith(receiverAddress)).isTrue();
    assertThat(connectionDetails.sharedSecret().key()).isEqualTo(
        connectionGenerator.deriveSecretFromAddress(serverSecret, connectionDetails.destinationAddress()));
  }

  @Test(expected = StreamException.class)
  public void assertErrorsIfCannotDeriveAddress() {
    InterledgerAddress receiverAddress = InterledgerAddress.of("example.receiver")
        .with("55412631d66b49073b0c36e17a29ba266164fc508bd3eb1c8fc718a02907ce01");

    try {
      connectionGenerator.deriveSecretFromAddress(serverSecret, receiverAddress);
    } catch (StreamException e) {
      assertThat(e.getMessage()).isEqualTo(
          "Invalid Receiver Address (should have been 32 byte long): InterledgerAddress{value=example.receiver.55412631d66b49073b0c36e17a29ba266164fc508bd3eb1c8fc718a02907ce01}");
      throw e;
    }
  }
}
