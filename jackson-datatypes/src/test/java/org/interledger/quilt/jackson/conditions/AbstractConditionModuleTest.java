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

import org.interledger.core.Condition;
import org.interledger.core.Fulfillment;
import org.interledger.quilt.jackson.conditions.ConditionModule;
import org.interledger.quilt.jackson.conditions.Encoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;

import java.security.Provider;
import java.security.Security;
import java.util.Objects;

/**
 * Validates the functionality of {@link ConditionModule}.
 */
public abstract class AbstractConditionModuleTest {

  /**
   * Need to add BouncyCastle so we have a provider that supports SHA256withRSA/PSS signatures
   */
  static {
    Provider bc = new BouncyCastleProvider();
    Security.addProvider(bc);
  }

  protected ObjectMapper objectMapper;
  protected Encoding encodingToUse;
  protected String expectedEncodedValue;

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public AbstractConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    this.encodingToUse = Objects.requireNonNull(encodingToUse);
    this.expectedEncodedValue = Objects.requireNonNull(expectedEncodedValue);
  }

  //////////////////
  // Protected Helpers
  //////////////////


  protected static Condition constructPreimageCondition() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return Condition.of(preimage);
  }

  protected static Fulfillment constructPreimageFulfillment() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return Fulfillment.of(preimage);
  }

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new ConditionModule(encodingToUse));
  }

}
