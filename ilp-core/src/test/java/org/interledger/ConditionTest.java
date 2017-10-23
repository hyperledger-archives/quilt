package org.interledger;

import static org.junit.Assert.assertArrayEquals;

import org.interledger.cryptoconditions.PreimageSha256Condition;

import org.junit.Test;

public class ConditionTest {

  @Test
  public final void test() {

    byte[] hash = new byte[32];
    assertArrayEquals("Hash is invalid.",
        hash,
        new PreimageSha256Condition(32, hash).getFingerprint());
  }

}
