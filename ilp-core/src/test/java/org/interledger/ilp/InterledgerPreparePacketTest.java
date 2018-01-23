package org.interledger.ilp;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.interledger.InterledgerAddress;

import org.junit.Test;

/**
 * Unit tests for {@link InterledgerPreparePacket} and {@link InterledgerPaymentBuilder}.
 */
public class InterledgerPreparePacketTest {

  @Test
  public void testBuild() throws Exception {
    final InterledgerAddress destination = mock(InterledgerAddress.class);
    byte[] data = new byte[]{127};

    final InterledgerPreparePacket interledgerPreparePacket =
        InterledgerPreparePacket.builder().destination(destination)
            .data(data).build();

    assertThat(interledgerPreparePacket.getDestination(), is(destination));
    assertThat(interledgerPreparePacket.getData(), is(data));
  }

  @Test
  public void testBuildWithNullValues() throws Exception {
    try {
      InterledgerPreparePacket.builder().build();
      fail();
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    try {
      InterledgerPreparePacket.builder().destination(mock(InterledgerAddress.class))
          .build();
      fail();
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    try {
      InterledgerPreparePacket.builder().destination(mock(InterledgerAddress.class))
          .build();
      fail();
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    final InterledgerPreparePacket interledgerPreparePacket =
        InterledgerPreparePacket.builder().destination(mock(InterledgerAddress.class))
            .data(new byte[]{}).build();
    assertThat(interledgerPreparePacket, is(not(nullValue())));
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    final InterledgerAddress destination = mock(InterledgerAddress.class);
    byte[] data = new byte[]{127};

    final InterledgerPreparePacket interledgerPreparePacket1 =
        InterledgerPreparePacket.builder().destination(destination)
            .data(data).build();

    final InterledgerPreparePacket interledgerPreparePacket2 =
        InterledgerPreparePacket.builder().destination(destination)
            .data(data).build();

    assertTrue(interledgerPreparePacket1.equals(interledgerPreparePacket2));
    assertTrue(interledgerPreparePacket2.equals(interledgerPreparePacket1));
    assertTrue(interledgerPreparePacket1.hashCode() == interledgerPreparePacket2.hashCode());

    final InterledgerPreparePacket interledgerPreparePacketOther = InterledgerPreparePacket.builder()
        .destination(destination)
        .data(data).build();

    assertFalse(interledgerPreparePacket1.equals(interledgerPreparePacketOther));
    assertFalse(interledgerPreparePacketOther.equals(interledgerPreparePacket1));
    assertFalse(interledgerPreparePacket1.hashCode() == interledgerPreparePacketOther.hashCode());
  }

}
