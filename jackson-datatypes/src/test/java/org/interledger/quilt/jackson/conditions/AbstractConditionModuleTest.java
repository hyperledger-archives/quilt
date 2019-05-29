package org.interledger.quilt.jackson.conditions;

/*-
 * ========================LICENSE_START=================================
 * Interledger Jackson Datatypes
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

import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;

import java.util.Objects;

/**
 * Validates the functionality of {@link ConditionModule}.
 */
public abstract class AbstractConditionModuleTest {

  private static final String PREIMAGE = "roads? where we're going we don'";

  protected ObjectMapper objectMapper;
  protected String expectedEncodedValue;
  private Encoding encodingToUse;

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  AbstractConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    this.encodingToUse = Objects.requireNonNull(encodingToUse);
    this.expectedEncodedValue = Objects.requireNonNull(expectedEncodedValue);
  }

  static InterledgerCondition constructPreimageCondition() {
    final byte[] preimage = PREIMAGE.getBytes();
    return InterledgerCondition.of(preimage);
  }

  //////////////////
  // Protected Helpers
  //////////////////

  static InterledgerFulfillment constructPreimageFulfillment() {
    final byte[] preimage = PREIMAGE.getBytes();
    return InterledgerFulfillment.of(preimage);
  }

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper().registerModule(new ConditionModule(encodingToUse));
  }

}
