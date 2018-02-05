package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link ThresholdFactory}.
 */
public class ThresholdFactoryTest extends AbstractFactoryTest {

  @Test(expected = NullPointerException.class)
  public void oneOfTwoConditionWithNullFirst() {
    try {
      ThresholdFactory.oneOfTwoCondition(null, subcondition2);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition1 must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void oneOfTwoConditionWithNullSecond() {
    try {
      ThresholdFactory.oneOfTwoCondition(subcondition1, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition2 must not be null!"));
      throw e;
    }
  }

  /**
   * Test {@link ThresholdFactory#oneOfTwoCondition(Condition, Condition)}.
   */
  @Test
  public void oneOfTwoCondition() {
    // Create a control threshold fulfillment that has enough fulfillments (one) to satisfy the
    // threshold of a 1-of-2 condition.
    final ThresholdSha256Fulfillment thresholdFulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition2),
        Lists.newArrayList(subfulfillment1)
    );

    {
      // Verify that a #oneOfTwoCondition verifies the fulfillment above.
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .oneOfTwoCondition(subcondition1, subcondition2);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(true));
      assertThat(thresholdFulfillment.verify(thresholdCondition), is(true));
    }

    {
      // Verify that a #oneOfTwoCondition verifies the fulfillment above.
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .oneOfTwoCondition(subcondition1, subcondition1);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
      assertThat(thresholdFulfillment.verify(thresholdCondition), is(false));
    }

    {
      // Simulate creating a condition with two of the same sub-conditions.
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .oneOfTwoCondition(subcondition2, subcondition2);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
      assertThat(thresholdFulfillment.verify(thresholdCondition), is(false));
    }
  }

  @Test(expected = NullPointerException.class)
  public void twoOfTwoConditionWithNullFirst() {
    try {
      ThresholdFactory.twoOfTwoCondition(null, subcondition2);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition1 must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void twoOfTwoConditionWithSecondNull() {
    try {
      ThresholdFactory.twoOfTwoCondition(subcondition1, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition2 must not be null!"));
      throw e;
    }
  }

  @Test
  public void twoOfTwoCondition() {

    // Create a control threshold fulfillment that has enough fulfillments (two) to satisfy the
    // threshold of a 2-of-2 condition.
    final ThresholdSha256Fulfillment thresholdFulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(),
        Lists.newArrayList(subfulfillment1, subfulfillment2)
    );

    assertThat(thresholdFulfillment.getSubconditions().size(), is(0));
    assertThat(thresholdFulfillment.getSubfulfillments().size(), is(2));

    {
      // Simulate creating a condition with one of each of the required sub-conditions.
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .twoOfTwoCondition(subcondition1, subcondition2);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    {
      // Simulate creating a condition with two of the same sub-conditions.
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .twoOfTwoCondition(subcondition1, subcondition1);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    {
      // Simulate creating a condition with two of the same sub-conditions.
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .twoOfTwoCondition(subcondition2, subcondition2);
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
  }

  @Test(expected = NullPointerException.class)
  public void mOfNWithNullSubConditionsList() {
    try {
      ThresholdFactory.constructMOfNCondition(1, 2, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("subconditions must not be null!"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void mOfNWithNegativeThreshold() {
    try {
      ThresholdFactory.constructMOfNCondition(-1, 2, Lists.newArrayList(subcondition1, subcondition2));
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Threshold must not be negative!"));
      throw e;
    }
  }

  @Test
  public void mOfNWithZeroThreshold() {
    // Create a control threshold fulfillment that has no fulfillments (0) to satisfy the
    // threshold of a 0-of-3 condition.
    final ThresholdSha256Fulfillment thresholdFulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition1, subcondition2, subcondition3),
        Lists.newArrayList()
    );

    final ThresholdSha256Condition thresholdCondition = ThresholdFactory
        .constructMOfNCondition(0, 3, Lists.newArrayList(subcondition1, subcondition2, subcondition3));
    assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void mOfNWithIncorrectNumSubConditions() {
    try {
      ThresholdFactory.constructMOfNCondition(0, 2, Lists.newArrayList(subcondition1));
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Number of sub-conditions must equal 2!"));
      throw e;
    }
  }

  /**
   * Test {@link ThresholdFactory#constructMOfNCondition(int, int, List)}.
   */
  @Test
  public void mOfNConditionTest() {

    // Create a control threshold fulfillment that has enough fulfillments (3) to satisfy the
    // threshold of a 3-of-5 condition.
    final ThresholdSha256Fulfillment thresholdFulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition1, subcondition2),
        Lists.newArrayList(subfulfillment3, subfulfillment4, subfulfillment5)
    );

    assertThat(thresholdFulfillment.getSubconditions().size(), is(2));
    assertThat(thresholdFulfillment.getSubfulfillments().size(), is(3));

    {
      // Test a condition with too low of a threshold
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .constructMOfNCondition(1, 5, Lists.newArrayList(
              subcondition1, subcondition2, subcondition3, subcondition4, subcondition5
          ));
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    {
      // Test a condition with too low of a threshold
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .constructMOfNCondition(2, 5, Lists.newArrayList(
              subcondition1, subcondition2, subcondition3, subcondition4, subcondition5
          ));
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    {
      // Test a condition with the correct threshold
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .constructMOfNCondition(3, 5, Lists.newArrayList(
              subcondition1, subcondition2, subcondition3, subcondition4, subcondition5
          ));
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    {
      // Test a condition with too high of a threshold
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .constructMOfNCondition(4, 5, Lists.newArrayList(
              subcondition1, subcondition2, subcondition3, subcondition4, subcondition5
          ));
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    {
      // Test a condition with too high of a threshold
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .constructMOfNCondition(5, 5, Lists.newArrayList(
              subcondition1, subcondition2, subcondition3, subcondition4, subcondition5
          ));
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    {
      // Test a condition with the correct threshold, but wrong conditions
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .constructMOfNCondition(3, 5, Lists.newArrayList(
              subcondition1, subcondition1, subcondition1, subcondition1, subcondition1
          ));
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    {
      // Test a condition with the correct threshold, but wrong conditions
      final ThresholdSha256Condition thresholdCondition = ThresholdFactory
          .constructMOfNCondition(3, 5, Lists.newArrayList(
              subcondition1, subcondition2, subcondition3, subcondition4, subcondition4
          ));
      assertThat(thresholdFulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
  }

  @Test(expected = NullPointerException.class)
  public void oneOfTwoFulfillmentWithFirstNull() {
    try {
      ThresholdFactory.oneOfTwoFulfillment(null, subcondition1);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("fulfillment must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void oneOfTwoFulfillmentWithSecondNull() {
    try {
      ThresholdFactory.oneOfTwoFulfillment(subfulfillment1, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("condition must not be null!"));
      throw e;
    }
  }

  /**
   * Test {@link ThresholdFactory#oneOfTwoFulfillment(Fulfillment, Condition)}.
   */
  @Test
  public void oneOfTwoFulfillment() {

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
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .oneOfTwoFulfillment(subfulfillment1, subcondition2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    {
      // Simulate publishing a fulfillment with the other 1 of the corresponding sub-fulfillments.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .oneOfTwoFulfillment(subfulfillment2, subcondition1);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    // Negative Tests
    {
      // Simulate publishing a fulfillment with one fulfillment and its corresponding thresholdCondition.
      final ThresholdSha256Fulfillment fulfillment1 = ThresholdFactory
          .oneOfTwoFulfillment(subfulfillment1, subcondition1);
      assertThat(fulfillment1.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with one fulfillment and its corresponding thresholdCondition.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .oneOfTwoFulfillment(subfulfillment2, subcondition2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with one fulfillment and a random sub-condition, unrelated
      // to the actual sub-conditions used in the main threshold fulfillment.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .oneOfTwoFulfillment(subfulfillment2,
              PreimageSha256Fulfillment.from("foo".getBytes()).getCondition());
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with a random sub-fulfillment, unrelated
      // to the actual sub-conditions used in the main threshold fulfillment.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .oneOfTwoFulfillment(PreimageSha256Fulfillment.from("foo".getBytes()), subcondition2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
  }

  @Test(expected = NullPointerException.class)
  public void twoOfTwoFulfillmentWithFirstNull() {
    try {
      ThresholdFactory.twoOfTwoFulfillment(null, subfulfillment2);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("fulfillment1 must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void twoOfTwoFulfillmentWithSecondNull() {
    try {
      ThresholdFactory.twoOfTwoFulfillment(subfulfillment1, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("fulfillment2 must not be null!"));
      throw e;
    }
  }

  @Test
  public void twoOfTwoFulfillment() {

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
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .twoOfTwoFulfillment(subfulfillment1, subfulfillment2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    {
      // Simulate publishing a fulfillment with the other 1 of the corresponding sub-fulfillments.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .twoOfTwoFulfillment(subfulfillment2, subfulfillment1);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    // Negative Tests
    {
      // Simulate publishing a fulfillment with one fulfillment and another that unrelated fulfillment.
      final ThresholdSha256Fulfillment fulfillment1 = ThresholdFactory
          .twoOfTwoFulfillment(subfulfillment1, subfulfillment3);
      assertThat(fulfillment1.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with one fulfillment twice.
      final ThresholdSha256Fulfillment fulfillment1 = ThresholdFactory
          .twoOfTwoFulfillment(subfulfillment1, subfulfillment1);
      assertThat(fulfillment1.verify(thresholdCondition, "".getBytes()), is(false));
    }

  }

  @Test(expected = NullPointerException.class)
  public void mOfNFulfillmentWithFirstNull() {
    try {
      ThresholdFactory.constructMOfNFulfillment(1, 2, null, Lists.newArrayList());
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("subconditions must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void mOfNFulfillmentWithSecondNull() {
    try {
      ThresholdFactory.constructMOfNFulfillment(1, 2, Lists.newArrayList(), null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("subfulfillments must not be null!"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void mOfNFulfillmenttsWithNegativeThreshold() {
    try {
      ThresholdFactory.constructMOfNFulfillment(-1, 2, Lists.newArrayList(),
          Lists.newArrayList(subfulfillment1, subfulfillment2));
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("thresholdM (Threshold) must not be negative!"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void mOfNFulfillmentWithIncorrectTotalN() {
    try {
      ThresholdFactory.constructMOfNFulfillment(1, 2,
          Lists.newArrayList(),
          Lists.newArrayList(subfulfillment1, subfulfillment2, subfulfillment3)
      );
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
          is("The combined number of sub-conditions and sub-fulfillments must equal 2!"));
      throw e;
    }
  }

  /**
   * Test {@link ThresholdFactory#constructMOfNFulfillment(int, int, List, List)} using a 1-of-2
   * condition/fulfillment pair.
   */
  @Test
  public void mOfNFulfillmentForOneOfTwo() {

    // The condition to fulfill...
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
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .oneOfTwoFulfillment(subfulfillment1, subcondition2);
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    {
      // Simulate publishing a fulfillment with the other 1 of the corresponding sub-fulfillments.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .constructMOfNFulfillment(1, 2,
              Lists.newArrayList(subcondition1),
              Lists.newArrayList(subfulfillment2)
          );
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(true));
    }

    // Negative Tests
    {
      // Simulate publishing a fulfillment with one fulfillment and its corresponding thresholdCondition.
      final ThresholdSha256Fulfillment fulfillment1 = ThresholdFactory
          .constructMOfNFulfillment(1, 2,
              Lists.newArrayList(subcondition1),
              Lists.newArrayList(subfulfillment1)
          );
      assertThat(fulfillment1.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with one fulfillment and its corresponding thresholdCondition.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .constructMOfNFulfillment(1, 2,
              Lists.newArrayList(subcondition2),
              Lists.newArrayList(subfulfillment2)
          );
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with one fulfillment and a random sub-condition, unrelated
      // to the actual sub-conditions used in the main threshold fulfillment.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .constructMOfNFulfillment(1, 2,
              Lists.newArrayList(PreimageSha256Fulfillment.from("foo".getBytes()).getCondition()),
              Lists.newArrayList(subfulfillment2)
          );
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Simulate publishing a fulfillment with a random sub-fulfillment, unrelated
      // to the actual sub-conditions used in the main threshold fulfillment.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .constructMOfNFulfillment(1, 2,
              Lists.newArrayList(subcondition1),
              Lists.newArrayList(PreimageSha256Fulfillment.from("foo".getBytes()))
          );
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    // Test more than enough fulfillments...
    {
      // Simulate publishing a fulfillment with a random sub-fulfillment, unrelated
      // to the actual sub-conditions used in the main threshold fulfillment.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .constructMOfNFulfillment(1, 2,
              Lists.newArrayList(),
              Lists.newArrayList(subfulfillment1, subfulfillment2)
          );
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }

    {
      // Simulate publishing a fulfillment with a random sub-fulfillment, unrelated
      // to the actual sub-conditions used in the main threshold fulfillment.
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .constructMOfNFulfillment(2, 2,
              Lists.newArrayList(),
              Lists.newArrayList(subfulfillment1, subfulfillment1)
          );
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Test not enough fulfillments...
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .constructMOfNFulfillment(1, 2,
              Lists.newArrayList(subcondition1, subcondition2),
              Lists.newArrayList()
          );
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
    {
      // Test not enough fulfillments...
      final ThresholdSha256Fulfillment fulfillment = ThresholdFactory
          .constructMOfNFulfillment(1, 2,
              Lists.newArrayList(subcondition1, subcondition2),
              Lists.newArrayList()
          );
      assertThat(fulfillment.verify(thresholdCondition, "".getBytes()), is(false));
    }
  }

}