package org.interledger.stream;

import static org.mockito.Mockito.mock;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.crypto.SharedSecret;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link StreamConnectionId}.
 */
public class StreamConnectionIdTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testConstructorWithNullSharedSecret() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("sharedSecret must not be null");
    StreamConnectionId.from(mock(InterledgerAddress.class), null);
  }

  @Test
  public void testConstructorWithAddressAndSecret() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("receiverAddress must not be null");
    StreamConnectionId.from(null, SharedSecret.of(new byte[32]));
  }

}
