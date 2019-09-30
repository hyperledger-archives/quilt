package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Arrays;

public class InterledgerFulfillmentTest {

  private static final byte[] BYTES_1 = new byte[] {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1
  };

  private static final byte[] BYTES_2 = new byte[] {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 2
  };

  private static final byte[] OVERFLOW_BYTES = new byte[] {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2
  };

  private static final byte[] UNDERFLOW_BYTES = new byte[] {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
      0
  };

  private static InterledgerCondition COND_1 = InterledgerCondition.of(new byte[] {
      -36, -79, -84, 74, 93, -29, 112, -54, -48, -111,
      -63, 63, 19, -82, -30, -7, 54, -62, 120, -6,
      5, -46, 100, 101, 60, 12, 19, 33, -123, 42,
      53, -24
  });

  private static InterledgerCondition COND_2 = InterledgerCondition.of(new byte[] {
      -1, -121, 82, -4, 108, 78, -100, 121, -32, 83,
      112, 112, 90, -99, -81, -98, -69, 65, 3, -125,
      123, 121, 26, -68, -99, -31, 48, 107, -98, -52,
      -120, -28
  });


  @Test
  public void testGetBytes() {
    assertThat(InterledgerFulfillment.of(BYTES_1).getPreimage()).isEqualTo(BYTES_1);
  }


  @Test
  public void testGetCondition() {
    final InterledgerCondition condition1 = InterledgerFulfillment.of(BYTES_1).getCondition();
    final InterledgerCondition condition2 = InterledgerFulfillment.of(BYTES_2).getCondition();

    assertThat(condition1).isEqualTo(COND_1);
    assertThat(condition2).isEqualTo(COND_2);

  }

  @Test
  public void testEquals() {
    final InterledgerFulfillment fulfillment = InterledgerFulfillment.of(BYTES_1);
    final byte[] copyOfBytes1 = Arrays.copyOf(BYTES_1, 32);
    assertThat(fulfillment).isEqualTo(fulfillment); //Same object
    assertThat(fulfillment).isEqualTo(InterledgerFulfillment.of(BYTES_1)); //Same array as input
    assertThat(fulfillment).isEqualTo(InterledgerFulfillment.of(copyOfBytes1)); //Equal arrays as input

    final InterledgerFulfillment otherFulfillment = InterledgerFulfillment.of(new byte[32]);
    assertThat(otherFulfillment).isNotEqualTo(fulfillment);
    assertThat(otherFulfillment).isNotEqualTo(InterledgerFulfillment.of(BYTES_1));
    assertThat(otherFulfillment).isNotEqualTo(InterledgerFulfillment.of(copyOfBytes1));
  }

  @Test
  public void testHashcode() {
    final InterledgerFulfillment fulfillment = InterledgerFulfillment.of(BYTES_1);
    final byte[] copyOfBytes1 = Arrays.copyOf(BYTES_1, 32);

    // Same object
    assertThat(fulfillment.hashCode()).isEqualTo(fulfillment.hashCode());

    // Same array as input
    assertThat(fulfillment.hashCode()).isEqualTo(InterledgerFulfillment.of(BYTES_1).hashCode());

    // Equal arrays as input
    assertThat(fulfillment.hashCode()).isEqualTo(InterledgerFulfillment.of(copyOfBytes1).hashCode());

    final InterledgerFulfillment otherFulfillment = InterledgerFulfillment.of(new byte[32]);
    assertThat(otherFulfillment.hashCode()).isNotEqualTo(fulfillment.hashCode());
    assertThat(otherFulfillment.hashCode()).isNotEqualTo(InterledgerFulfillment.of(BYTES_1).hashCode());
    assertThat(otherFulfillment.hashCode()).isNotEqualTo(InterledgerFulfillment.of(copyOfBytes1).hashCode());
  }

  @Test
  public void testNotEquals() {
    assertThat(InterledgerFulfillment.of(BYTES_1)).isNotEqualTo(InterledgerFulfillment.of(BYTES_2));
  }

  @Test
  public void testCompare() {
    assertThat(InterledgerFulfillment.of(BYTES_1).compareTo(InterledgerFulfillment.of(BYTES_1)) == 0).isTrue();
    assertThat(InterledgerFulfillment.of(BYTES_1).compareTo(InterledgerFulfillment.of(BYTES_2)) < 0).isTrue();
    assertThat(InterledgerFulfillment.of(BYTES_2).compareTo(InterledgerFulfillment.of(BYTES_1)) > 0).isTrue();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnderFlow() {
    InterledgerFulfillment.of(UNDERFLOW_BYTES);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOverFlow() {
    InterledgerFulfillment.of(OVERFLOW_BYTES);
  }

}
