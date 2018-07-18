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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.Arrays;

public class InterledgerConditionTest {

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


  @Test
  public void testGetBytes() {
    assertArrayEquals(InterledgerCondition.of(BYTES_1).getHash(), BYTES_1);
  }

  @Test
  public void testEquals() {
    InterledgerCondition condition = InterledgerCondition.of(BYTES_1);
    byte[] copyOfBytes1 = Arrays.copyOf(BYTES_1, 32);

    assertEquals(condition, condition); //Same object
    assertEquals(condition, InterledgerCondition.of(BYTES_1)); //Same array as input
    assertEquals(condition, InterledgerCondition.of(copyOfBytes1)); //Equal arrays as input
  }

  @Test
  public void testNotEquals() {
    assertNotEquals(InterledgerCondition.of(BYTES_1), InterledgerCondition.of(BYTES_2));
  }

  @Test
  public void testCompare() {
    assertTrue(
        InterledgerCondition.of(BYTES_1).compareTo(InterledgerCondition.of(BYTES_1)) == 0
    );
    assertTrue(
        InterledgerCondition.of(BYTES_1).compareTo(InterledgerCondition.of(BYTES_2)) < 0
    );
    assertTrue(
        InterledgerCondition.of(BYTES_2).compareTo(InterledgerCondition.of(BYTES_1)) > 0
    );
  }

  @Test
  public void testHashCode() {
    assertTrue(InterledgerCondition.of(BYTES_1).hashCode() == InterledgerCondition
        .of(BYTES_1).hashCode());
    assertFalse(InterledgerCondition.of(BYTES_1).hashCode() == InterledgerCondition
        .of(BYTES_2).hashCode());
    assertFalse(InterledgerCondition.of(BYTES_2).hashCode() == InterledgerCondition
        .of(BYTES_1).hashCode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnderFlow() {
    InterledgerCondition.of(UNDERFLOW_BYTES);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOverFlow() {
    InterledgerCondition.of(OVERFLOW_BYTES);
  }

}
