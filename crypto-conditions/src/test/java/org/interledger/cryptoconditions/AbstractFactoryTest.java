package org.interledger.cryptoconditions;

import org.junit.Before;

/**
 * Abstract helper class for various test implementations.
 */
public abstract class AbstractFactoryTest extends AbstractCryptoConditionTest {

  protected PreimageSha256Fulfillment subfulfillment1;
  protected PreimageSha256Condition subcondition1;

  protected PreimageSha256Fulfillment subfulfillment2;
  protected PreimageSha256Condition subcondition2;

  protected PreimageSha256Fulfillment subfulfillment3;
  protected PreimageSha256Condition subcondition3;

  protected PreimageSha256Fulfillment subfulfillment4;
  protected PreimageSha256Condition subcondition4;

  protected PreimageSha256Fulfillment subfulfillment5;
  protected PreimageSha256Condition subcondition5;

  /**
   * Setup test data.
   */
  @Before
  public void setup() {
    // Construct Preimage Fulfillment/Condition #1
    this.subfulfillment1 = PreimageSha256Fulfillment.from(
        "Roads? Where we're going, we don't need roads.".getBytes()
    );
    this.subcondition1 = subfulfillment1.getDerivedCondition();

    // Construct Preimage Fulfillment/Condition #2
    this.subfulfillment2 = PreimageSha256Fulfillment.from(
        "Nobody calls me chicken!".getBytes()
    );
    this.subcondition2 = subfulfillment2.getDerivedCondition();

    // Construct Preimage Fulfillment/Condition #3
    this.subfulfillment3 = PreimageSha256Fulfillment.from(
        "Great Scott!".getBytes()
    );
    this.subcondition3 = subfulfillment3.getDerivedCondition();

    // Construct Preimage Fulfillment/Condition #4
    this.subfulfillment4 = PreimageSha256Fulfillment.from(
        "Are you telling me that you built a time machine".getBytes()
    );
    this.subcondition4 = subfulfillment4.getDerivedCondition();

    // Construct Preimage Fulfillment/Condition #5
    this.subfulfillment5 = PreimageSha256Fulfillment.from(
        "I finally invent something that works!".getBytes()
    );
    this.subcondition5 = subfulfillment5.getDerivedCondition();
  }
}