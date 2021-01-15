package org.interledger.stream;

import static org.mockito.Mockito.mock;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.stream.crypto.StreamSharedSecret;

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

    SharedSecret nullSharedSecret = null;
    StreamConnectionId.from(mock(InterledgerAddress.class), nullSharedSecret);
  }

  @Test
  public void testConstructorWithNullStraemSharedSecret() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("streamSharedSecret must not be null");

    StreamSharedSecret nullStreamSharedSecret = null;
    StreamConnectionId.from(mock(InterledgerAddress.class), nullStreamSharedSecret);
  }

  @Test
  public void testConstructorWithAddressAndSecret() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("receiverAddress must not be null");
    StreamConnectionId.from(null, SharedSecret.of(new byte[32]));
  }

}
