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
  public void testGetHash() {
    assertThat(InterledgerCondition.of(BYTES_1).getHash()).isEqualTo(BYTES_1);
  }

  @Test
  public void testEquals() {
    InterledgerCondition condition = InterledgerCondition.of(BYTES_1);
    byte[] copyOfBytes1 = Arrays.copyOf(BYTES_1, 32);

    assertThat(condition).isEqualTo(condition); //Same object
    assertThat(condition).isEqualTo(InterledgerCondition.of(BYTES_1)); //Same array as input
    assertThat(condition).isEqualTo(InterledgerCondition.of(copyOfBytes1)); //Equal arrays as input
  }

  @Test
  public void testNotEquals() {
    assertThat(InterledgerCondition.of(BYTES_1)).isNotEqualTo(InterledgerCondition.of(BYTES_2));
  }

  @Test
  public void testCompare() {
    assertThat(InterledgerCondition.of(BYTES_1).compareTo(InterledgerCondition.of(BYTES_1)) == 0).isTrue();
    assertThat(InterledgerCondition.of(BYTES_1).compareTo(InterledgerCondition.of(BYTES_2)) < 0).isTrue();
    assertThat(InterledgerCondition.of(BYTES_2).compareTo(InterledgerCondition.of(BYTES_1)) > 0).isTrue();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnderFlow() {
    InterledgerCondition.of(UNDERFLOW_BYTES);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOverFlow() {
    InterledgerCondition.of(OVERFLOW_BYTES);
  }

  @Test
  @SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:LocalVariableName"})
  public void testHashCode() {
    int BYTES_1_HASHCODE = InterledgerCondition.of(BYTES_1).hashCode();
    int BYTES_1_HASHCODE_1 = InterledgerCondition.of(BYTES_1).hashCode();
    int BYTES_2_HASHCODE = InterledgerCondition.of(BYTES_2).hashCode();

    assertThat(BYTES_1_HASHCODE == BYTES_1_HASHCODE_1).isTrue();
    assertThat(BYTES_1_HASHCODE == BYTES_2_HASHCODE).isFalse();
  }

  @Test
  public void testFrom() {
    final InterledgerCondition condition1 = InterledgerCondition.of(BYTES_1);
    final InterledgerCondition condition2 = InterledgerCondition.from(condition1);

    assertThat(condition1).isEqualTo(condition2);
  }

  @Test
  public void testToString() {
    final InterledgerCondition condition1 = InterledgerCondition.of(BYTES_1);
    assertThat(condition1.toString()).isEqualTo("Condition{hash=AAECAwQFBgcICQABAgMEBQYHCAkAAQIDBAUGBwgJAAE=}");
  }

}
