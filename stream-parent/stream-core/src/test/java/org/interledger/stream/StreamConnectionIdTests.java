package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.crypto.StreamSharedSecret;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link StreamConnectionId}.
 */
public class StreamConnectionIdTests {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testFromWithNullReceiver() {
    expectedException.expect(NullPointerException.class);
    StreamConnectionId.from(null, mock(StreamSharedSecret.class));
  }

  @Test
  public void testFromWithNullStreamSharedSecret() {
    expectedException.expect(NullPointerException.class);
    StreamSharedSecret nullStreamSharedSecret = null;
    StreamConnectionId.from(mock(InterledgerAddress.class), nullStreamSharedSecret);
  }

  @Test
  public void testFromWithStreamSharedSecret() {
    StreamSharedSecret streamSharedSecret = StreamSharedSecret.of(new byte[32]);
    assertThat(StreamConnectionId.from(InterledgerAddress.of("example.foo"), streamSharedSecret))
      .isEqualTo(StreamConnectionId.of("246307596a10c1ba057f56cd6d588ed0d11cf3f8817c937265e93950af53751f"));
  }

}
