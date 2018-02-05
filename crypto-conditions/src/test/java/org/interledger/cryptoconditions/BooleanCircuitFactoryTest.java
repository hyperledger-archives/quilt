package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.Lists;
import org.junit.Test;

/**
 * Unit tests for {@link BooleanCircuitFactory}.
 */
public class BooleanCircuitFactoryTest extends AbstractFactoryTest {

  @Test(expected = NullPointerException.class)
  public void orConditionWithFirstNull() {
    try {
      final Condition nullCondition = null;
      BooleanCircuitFactory.orConditionCircuit(nullCondition, subcondition2);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition1 must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void orConditionWithSecondNull() {
    try {
      final Condition nullCondition = null;
      BooleanCircuitFactory.orConditionCircuit(subcondition1, nullCondition);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition2 must not be null!"));
      throw e;
    }
  }

  @Test
  public void orCondition() {
    // Create a control threshold fulfillment that has enough fulfillments (one) to satisfy the
    // threshold of a 1-of-2 condition.
    final ThresholdSha256Fulfillment thresholdFulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition2),
        Lists.newArrayList(subfulfillment1)
    );

    {
      // Verify that a #oneOfTwoCondition verifies the fulfillment above.
      final ThresholdSha256Condition thresholdCondition = BooleanCircuitFactory
          .orConditionCircuit(subcondition1, subcondition2);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    {
      // Verify that a #oneOfTwoCondition verifies the fulfillment above.
      final ThresholdSha256Condition thresholdCondition = BooleanCircuitFactory
          .orConditionCircuit(subcondition1, subcondition1);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    {
      // Simulate creating a condition with two of the same sub-conditions.
      final ThresholdSha256Condition thresholdCondition = BooleanCircuitFactory
          .orConditionCircuit(subcondition2, subcondition2);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
  }

  @Test(expected = NullPointerException.class)
  public void andConditionWithFirstNull() {
    try {
      final Condition nullCondition = null;
      BooleanCircuitFactory.andConditionCircuit(nullCondition, subcondition2);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition1 must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void andConditionWithSecondNull() {
    try {
      final Condition nullCondition = null;
      BooleanCircuitFactory.andConditionCircuit(subcondition1, nullCondition);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition2 must not be null!"));
      throw e;
    }
  }

  @Test
  public void andCondition() {

    // Create a control threshold fulfillment that has enough fulfillments (two) to satisfy the
    // threshold of a 2-of-2 condition.
    final ThresholdSha256Fulfillment thresholdFulfillment = ThresholdFactory
        .twoOfTwoFulfillment(subfulfillment1, subfulfillment2);

    assertThat(thresholdFulfillment.getSubconditions().size(), is(0));
    assertThat(thresholdFulfillment.getSubfulfillments().size(), is(2));

    {
      // Simulate creating a condition with one of each of the required sub-conditions.
      final ThresholdSha256Condition thresholdCondition = BooleanCircuitFactory
          .andConditionCircuit(subcondition1, subcondition2);
      assertThat(thresholdFulfillment.verify(thresholdCondition), is(true));
    }

    {
      // Simulate creating a condition with two of the same sub-conditions.
      final ThresholdSha256Condition thresholdCondition = BooleanCircuitFactory
          .andConditionCircuit(subcondition1, subcondition1);
      assertThat(thresholdFulfillment.verify(thresholdCondition), is(false));
    }

    {
      // Simulate creating a condition with two of the same sub-conditions.
      final ThresholdSha256Condition thresholdCondition = BooleanCircuitFactory
          .andConditionCircuit(subcondition2, subcondition2);
      assertThat(thresholdFulfillment.verify(thresholdCondition), is(false));
    }

  }

  @Test(expected = NullPointerException.class)
  public void orFulfillmentWithFirstNull() {
    try {
      final Fulfillment nullFulfillment = null;
      BooleanCircuitFactory.orFulfillmentCircuit(nullFulfillment, subcondition1);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("fulfillment must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void orFulfillmentWithSecondNull() {
    try {
      final Condition nullCondition = null;
      BooleanCircuitFactory.orFulfillmentCircuit(subfulfillment1, nullCondition);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition must not be null!"));
      throw e;
    }
  }

  @Test
  public void orFulfillment() {
    // Adding two of the same condition is allowed...
    final ThresholdSha256Condition thresholdCondition = ThresholdSha256Condition.from(
        1, Lists.newArrayList(subcondition1, subcondition2)
    );

    assertThat(thresholdCondition.getSubtypes().contains(CryptoConditionType.PREIMAGE_SHA256),
        is(true));
    assertThat(thresholdCondition.getSubtypes().size(), is(1));
    assertThat(thresholdCondition.getFingerprintBase64Url(),
        is("QHaT8pDA7rr_SnKiVvZ-NoOX5GuNI-pUtT5loHDwdQg"));
    assertThat(thresholdCondition.getCost(), is(2094L));
    assertThat(thresholdCondition.getType(), is(CryptoConditionType.THRESHOLD_SHA256));
    {
      // Simulate publishing a fulfillment with 1 of the corresponding sub-fulfillments.
      final ThresholdSha256Fulfillment fulfillment = BooleanCircuitFactory
          .orFulfillmentCircuit(subfulfillment1, subcondition2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    {
      // Simulate publishing a fulfillment with the other 1 of the corresponding sub-fulfillments.
      final ThresholdSha256Fulfillment fulfillment = BooleanCircuitFactory
          .orFulfillmentCircuit(subfulfillment2, subcondition1);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    // Negative Tests
    {
      // Simulate publishing a fulfillment with one fulfillment and its corresponding thresholdCondition.
      final ThresholdSha256Fulfillment fulfillment1 = BooleanCircuitFactory
          .orFulfillmentCircuit(subfulfillment1, subcondition1);
      assertThat(fulfillment1.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with one fulfillment and its corresponding thresholdCondition.
      final ThresholdSha256Fulfillment fulfillment = BooleanCircuitFactory
          .orFulfillmentCircuit(subfulfillment2, subcondition2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with one fulfillment and a random sub-condition, unrelated
      // to the actual sub-conditions used in the main threshold fulfillment.
      final ThresholdSha256Fulfillment fulfillment = BooleanCircuitFactory
          .orFulfillmentCircuit(subfulfillment2,
              PreimageSha256Fulfillment.from("foo".getBytes()).getCondition());
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with a random sub-fulfillment, unrelated
      // to the actual sub-conditions used in the main threshold fulfillment.
      final ThresholdSha256Fulfillment fulfillment = BooleanCircuitFactory
          .orFulfillmentCircuit(PreimageSha256Fulfillment.from("foo".getBytes()), subcondition2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
  }

  @Test(expected = NullPointerException.class)
  public void andFulfillmentWithFirstNull() {
    try {
      BooleanCircuitFactory.andFulfillmentCircuit(null, subfulfillment2);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("fulfillment1 must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void andFulfillmentWithSecondNull() {
    try {
      BooleanCircuitFactory.andFulfillmentCircuit(subfulfillment1, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("fulfillment2 must not be null!"));
      throw e;
    }
  }

  @Test
  public void andFulfillment() {
    // Adding two of the same condition is allowed...
    final ThresholdSha256Condition thresholdCondition = ThresholdSha256Condition.from(
        2, Lists.newArrayList(subcondition1, subcondition2)
    );

    assertThat(thresholdCondition.getSubtypes().contains(CryptoConditionType.PREIMAGE_SHA256),
        is(true));
    assertThat(thresholdCondition.getSubtypes().size(), is(1));
    assertThat(thresholdCondition.getFingerprintBase64Url(),
        is("vo0OYr-3kha9LN85LY56wIrBaBt0MH_iqlqPiKO4t_U"));
    assertThat(thresholdCondition.getCost(), is(2118L));
    assertThat(thresholdCondition.getType(), is(CryptoConditionType.THRESHOLD_SHA256));
    {
      // Simulate publishing a fulfillment with both of the corresponding sub-fulfillments.
      final ThresholdSha256Fulfillment fulfillment = BooleanCircuitFactory
          .andFulfillmentCircuit(subfulfillment1, subfulfillment2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    {
      // Simulate publishing a fulfillment with the other 1 of the corresponding sub-fulfillments.
      final ThresholdSha256Fulfillment fulfillment = BooleanCircuitFactory
          .andFulfillmentCircuit(subfulfillment2, subfulfillment1);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    // Negative Tests
    {
      // Simulate publishing a fulfillment with one fulfillment and another that unrelated fulfillment.
      final ThresholdSha256Fulfillment fulfillment1 = BooleanCircuitFactory
          .andFulfillmentCircuit(subfulfillment1, subfulfillment3);
      assertThat(fulfillment1.verify(thresholdCondition, "".getBytes()), is(false));
    }
  }
}