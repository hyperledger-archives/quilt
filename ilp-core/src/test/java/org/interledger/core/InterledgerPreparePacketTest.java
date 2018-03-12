package org.interledger.core;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.interledger.cryptoconditions.PreimageSha256Condition;

import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;

/**
 * Unit tests for {@link InterledgerPreparePacket} and {@link InterledgerPreparePacketBuilder}.
 */
public class InterledgerPreparePacketTest {

  @Test
  public void testBuild() throws Exception {
    final InterledgerAddress destination = mock(InterledgerAddress.class);
    byte[] data = new byte[]{127};
    BigInteger amount = BigInteger.TEN;
    PreimageSha256Condition condition = PreimageSha256Condition.fromCostAndFingerprint(32,
        new byte[] {0,1,2,3,4,5,6,7,8,9,01,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2});
    Instant expiry = Instant.now().plusSeconds(30);

    final InterledgerPreparePacket interledgerPreparePacket =
        InterledgerPreparePacket.builder()
            .destination(destination)
            .amount(amount)
            .executionCondition(condition)
            .expiresAt(expiry)
            .data(data)
            .build();

    assertThat(interledgerPreparePacket.getDestination(), is(destination));
    assertThat(interledgerPreparePacket.getAmount(), is(amount));
    assertThat(interledgerPreparePacket.getExecutionCondition(), is(condition));
    assertThat(interledgerPreparePacket.getExpiresAt(), is(expiry));
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

    //No data
    try {
      InterledgerPreparePacket.builder()
          .destination(mock(InterledgerAddress.class))
          .amount(mock(BigInteger.class))
          .executionCondition(mock(PreimageSha256Condition.class))
          .expiresAt(Instant.now())
          .build();
    } catch (IllegalStateException e) {
      fail();
    }

    //No expiry
    try {
      InterledgerPreparePacket.builder()
          .destination(mock(InterledgerAddress.class))
          .amount(mock(BigInteger.class))
          .executionCondition(mock(PreimageSha256Condition.class))
          .build();
      fail();
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    //No condition
    try {
      InterledgerPreparePacket.builder()
          .destination(mock(InterledgerAddress.class))
          .amount(mock(BigInteger.class))
          .expiresAt(Instant.now())
          .build();
      fail();
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    //No amount
    try {
      InterledgerPreparePacket.builder()
          .destination(mock(InterledgerAddress.class))
          .executionCondition(mock(PreimageSha256Condition.class))
          .expiresAt(Instant.now())
          .build();
      fail();
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }


    //No destination
    try {
      InterledgerPreparePacket.builder()
          .amount(mock(BigInteger.class))
          .executionCondition(mock(PreimageSha256Condition.class))
          .expiresAt(Instant.now())
          .build();
      fail();
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    final InterledgerPreparePacket interledgerPreparePacket =
        InterledgerPreparePacket.builder()
            .destination(mock(InterledgerAddress.class))
            .amount(mock(BigInteger.class))
            .executionCondition(mock(PreimageSha256Condition.class))
            .expiresAt(Instant.now())
            .build();
    assertThat(interledgerPreparePacket, is(not(nullValue())));
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    final InterledgerAddress destination = mock(InterledgerAddress.class);
    byte[] data = new byte[]{127};
    BigInteger amount = BigInteger.TEN;
    PreimageSha256Condition condition = PreimageSha256Condition.fromCostAndFingerprint(32,
        new byte[] {0,1,2,3,4,5,6,7,8,9,01,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2});
    Instant expiry = Instant.now().plusSeconds(30);

    final InterledgerPreparePacket interledgerPreparePacket1 =
        InterledgerPreparePacket.builder()
            .destination(destination)
            .amount(amount)
            .executionCondition(condition)
            .expiresAt(expiry)
            .data(data)
            .build();

    final InterledgerPreparePacket interledgerPreparePacket2 =
        InterledgerPreparePacket.builder()
            .destination(destination)
            .amount(amount)
            .executionCondition(condition)
            .expiresAt(expiry)
            .data(data)
            .build();
    
    assertTrue(interledgerPreparePacket1.equals(interledgerPreparePacket2));
    assertTrue(interledgerPreparePacket2.equals(interledgerPreparePacket1));
    assertTrue(interledgerPreparePacket1.hashCode() == interledgerPreparePacket2.hashCode());

    final InterledgerPreparePacket interledgerPreparePacket3 = InterledgerPreparePacket.builder()
        .destination(destination)
        .amount(amount.add(BigInteger.ONE))
        .executionCondition(condition)
        .expiresAt(expiry)
        .data(data)
        .build();

    assertFalse(interledgerPreparePacket1.equals(interledgerPreparePacket3));
    assertFalse(interledgerPreparePacket3.equals(interledgerPreparePacket1));
    assertFalse(interledgerPreparePacket1.hashCode()
        == interledgerPreparePacket3.hashCode());
  }

}
