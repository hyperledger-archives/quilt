package org.interledger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;

import org.junit.Test;

import java.util.Base64;

public class FulfillmentTest {

  private static final byte[] TEST_PREIMAGE = new byte[32];
  private static final Condition TEST_CONDITION = new PreimageSha256Condition(32,
      Base64.getUrlDecoder().decode("Zmh6rfhivXdsj8GLjp-OIAiXFIVu4jOzkCpZHQ1fKSU"));

  // TODO These tests no longer work because the size validation is only done during encoding
  // Consider re-instating tests after refactoring crypto-conditions
  //  @Test(expected = IllegalArgumentException.class)
  //  public final void testSmallPreimage() {
  //    new PreimageSha256Fulfillment(new byte[31]);
  //  }

  // TODO These tests no longer work because the size validation is only done during encoding
  // Consider re-instating tests after refactoring crypto-conditions
  //  @Test(expected = IllegalArgumentException.class)
  //  public final void testBigPreimage() {
  //    new PreimageSha256Fulfillment(new byte[33]);
  //  }

  @Test(expected = NullPointerException.class)
  public final void testNullPreimage() {
    new PreimageSha256Fulfillment(null);
  }

  @Test()
  public final void testGetCondition() {

    Fulfillment fulfillment = new PreimageSha256Fulfillment(TEST_PREIMAGE);
    assertEquals("Wrong condition", TEST_CONDITION, fulfillment.getCondition());

  }

  @Test()
  public final void testValidate() {

    Fulfillment fulfillment = new PreimageSha256Fulfillment(TEST_PREIMAGE);
    assertTrue("Invalid condition", fulfillment.verify(TEST_CONDITION, new byte[]{}));

  }


}
