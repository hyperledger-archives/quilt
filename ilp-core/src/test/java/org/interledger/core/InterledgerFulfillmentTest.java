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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
    assertArrayEquals(InterledgerFulfillment.of(BYTES_1).getPreimage(), BYTES_1);
  }


  @Test
  public void testGetCondition() {

    InterledgerCondition condition1 = InterledgerFulfillment.of(BYTES_1).getCondition();
    InterledgerCondition condition2 = InterledgerFulfillment.of(BYTES_2).getCondition();

    assertEquals(condition1, COND_1);
    assertEquals(condition2, COND_2);

  }

  @Test
  public void testEquals() {
    InterledgerFulfillment fulfillment = InterledgerFulfillment.of(BYTES_1);
    byte[] copyOfBytes1 = Arrays.copyOf(BYTES_1, 32);

    assertEquals(fulfillment, fulfillment); //Same object
    assertEquals(fulfillment, InterledgerFulfillment.of(BYTES_1)); //Same array as input
    assertEquals(fulfillment, InterledgerFulfillment.of(copyOfBytes1)); //Equal arrays as input
  }

  @Test
  public void testNotEquals() {
    assertNotEquals(InterledgerFulfillment.of(BYTES_1), InterledgerFulfillment.of(BYTES_2));
  }

  @Test
  public void testCompare() {
    assertTrue(InterledgerFulfillment.of(BYTES_1).compareTo(InterledgerFulfillment.of(BYTES_1)) == 0);
    assertTrue(InterledgerFulfillment.of(BYTES_1).compareTo(InterledgerFulfillment.of(BYTES_2)) < 0);
    assertTrue(InterledgerFulfillment.of(BYTES_2).compareTo(InterledgerFulfillment.of(BYTES_1)) > 0);
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
