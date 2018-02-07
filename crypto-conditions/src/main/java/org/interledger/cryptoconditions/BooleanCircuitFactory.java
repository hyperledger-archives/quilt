package org.interledger.cryptoconditions;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A factory that can construct boolean-circuit representations from Crypto Condition primitives.
 */
public class BooleanCircuitFactory {

  // TODO: Construct a CircuitDefinition, see BigChainDB and slides from R.

  /**
   * <p>Construct a boolean circuit that will operate like an "OR" gate, allowiong {@link
   * Fulfillment#verify(Condition, byte[])} to return {@code true} when either supplied condition is
   * fulfilled, otherwise {@code false}.</p>
   *
   * @param condition1 The first sub-condition.
   * @param condition2 The second sub-condition.
   *
   * @return A {@link ThresholdSha256Condition} that can be fulfilled by supplying a Threshold
   *     Fulfillment containing only one of the two sub-fulillments that correspond to the inputs of
   *     this function.
   *
   * @see "https://en.wikipedia.org/wiki/OR_gate"
   */
  public static ThresholdSha256Condition orConditionCircuit(
      final Condition condition1, final Condition... conditions
  ) {
    Objects.requireNonNull(condition1, "condition1 must not be null!");
    Objects.requireNonNull(condition2, "condition2 must not be null!");

    return ThresholdFactory.constructMOfNCondition(1, 2,
        Stream.of(condition1, condition2).collect(Collectors.toList())
    );
  }

  /**
   * <p>Construct a Threshold Condition that can only be fulfilled when fulfillments for both of the
   * sub-conditions supplied by this function are present. This method constructs a 2-of-2 threshold
   * condition, which can be use to simulate a boolean-logic gate providing _AND_
   * functionality.</p>
   *
   * @param condition1 The first sub-condition.
   * @param condition2 The second sub-condition.
   *
   * @return A {@link ThresholdSha256Condition} that can be fulfilled by supplying a Threshold
   *     Fulfillment containing only one of the two sub-fulillments that correspond to the inputs of
   *     this function.
   *
   * @see "https://en.wikipedia.org/wiki/AND_gate"
   */
  public static ThresholdSha256Condition andConditionCircuit(
      final Condition condition1, final Condition condition2
  ) {
    Objects.requireNonNull(condition1, "condition1 must not be null!");
    Objects.requireNonNull(condition2, "condition2 must not be null!");

    return ThresholdFactory.constructMOfNCondition(2, 2,
        Stream.of(condition1, condition2).collect(Collectors.toList())
    );
  }

  /**
   * <p>Construct a Threshold Condition that can be fulfilled when at least one sub-fulfillment for
   * either of the sub-conditions supplied by this function are present. This method constructs a
   * 1-of-2 threshold condition, which can be use to simulate a boolean-logic gate providing _OR_
   * functionality.</p>
   *
   * @param fulfillment The fulfillment being fulfilled.
   * @param condition   The other sub-condition that is not fulfilled.
   *
   * @return A {@link ThresholdSha256Condition} that can be fulfilled by supplying a Threshold
   *     Fulfillment containing only one of the two sub-fulillments that correspond to the inputs of
   *     this function.
   *
   * @see "https://en.wikipedia.org/wiki/OR_gate"
   */
  public static ThresholdSha256Fulfillment orFulfillmentCircuit(
      final Fulfillment fulfillment, final Condition condition
  ) {
    Objects.requireNonNull(fulfillment, "fulfillment must not be null!");
    Objects.requireNonNull(condition, "condition must not be null!");

    return ThresholdFactory.constructMOfNFulfillment(1, 2,
        Stream.of(condition).collect(Collectors.toList()),
        Stream.of(fulfillment).collect(Collectors.toList())
    );
  }

  /**
   * <p>Construct a Threshold Fulfillment that can only be fulfilled when two sub-fulfillments are
   * present. This method constructs a 2-of-2 threshold fulfillment, which can be use to simulate a
   * boolean-logic gate providing _AND_ functionality.</p>
   *
   * @param fulfillment1 The first sub-fulfillment
   * @param fulfillment2 The second sub-fulfillment.
   *
   * @return A {@link ThresholdSha256Condition} that can be fulfilled by supplying a Threshold
   *     Fulfillment containing only one of the two sub-fulillments that correspond to the inputs of
   *     this function.
   *
   * @see "https://en.wikipedia.org/wiki/AND_gate"
   */
  public static ThresholdSha256Fulfillment andFulfillmentCircuit(
      final Fulfillment fulfillment1, final Fulfillment fulfillment2
  ) {
    Objects.requireNonNull(fulfillment1, "fulfillment1 must not be null!");
    Objects.requireNonNull(fulfillment2, "fulfillment2 must not be null!");

    return ThresholdFactory.constructMOfNFulfillment(
        2, 2,
        Collections.emptyList(),
        Stream.of(fulfillment1, fulfillment2).collect(Collectors.toList())
    );
  }

}
