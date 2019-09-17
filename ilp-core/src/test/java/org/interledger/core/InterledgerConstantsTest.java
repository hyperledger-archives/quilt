package org.interledger.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InterledgerConstantsTest {

  @Test
  public void allZerosJustLikeMyReportCard() {
    assertThat(InterledgerConstants.ALL_ZEROS_FULFILLMENT).isEqualTo(InterledgerFulfillment.of(new byte[32]));
    assertThat(InterledgerConstants.ALL_ZEROS_CONDITION).isEqualTo(InterledgerFulfillment.of(new byte[32]).getCondition());
  }
}
