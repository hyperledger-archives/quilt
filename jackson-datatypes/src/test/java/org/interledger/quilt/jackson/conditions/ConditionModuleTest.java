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

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.quilt.jackson.conditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.conditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.conditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.conditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.conditions.Encoding.HEX;

import org.interledger.core.InterledgerCondition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Validates the functionality of {@link ConditionModule}.
 */
@RunWith(Parameterized.class)
public class ConditionModuleTest extends AbstractConditionModuleTest {

  private static final String PREIMAGE_CONDITION_BYTES_HEX =
      "726F6164733F20776865726520776527726520676F696E6720776520646F6E27";
  private static final String PREIMAGE_CONDITION_BYTES_BASE64
      = "cm9hZHM/IHdoZXJlIHdlJ3JlIGdvaW5nIHdlIGRvbic=";
  private static final String PREIMAGE_CONDITION_BYTES_BASE64_WITHOUTPADDING
      = "cm9hZHM/IHdoZXJlIHdlJ3JlIGdvaW5nIHdlIGRvbic";
  private static final String PREIMAGE_CONDITION_BYTES_BASE64_URL
      = "cm9hZHM_IHdoZXJlIHdlJ3JlIGdvaW5nIHdlIGRvbic=";
  private static final String PREIMAGE_CONDITION_BYTES_BASE64_URL_WITHOUTPADDING
      = "cm9hZHM_IHdoZXJlIHdlJ3JlIGdvaW5nIHdlIGRvbic";

  private static InterledgerCondition CONDITION = constructPreimageCondition();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public ConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    super(encodingToUse, expectedEncodedValue);
  }

  /**
   * Get test parameters.
   *
   * @return the parameters for the tests
   */
  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleConditionTest constructor.
    return Arrays.asList(new Object[][] {
        {HEX, PREIMAGE_CONDITION_BYTES_HEX},
        {BASE64, PREIMAGE_CONDITION_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, PREIMAGE_CONDITION_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, PREIMAGE_CONDITION_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, PREIMAGE_CONDITION_BYTES_BASE64_URL_WITHOUTPADDING},
    });

  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final PreimageConditionContainer expectedContainer = new PreimageConditionContainer(CONDITION);

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json).isEqualTo(String.format("{\"condition\":\"%s\"}", expectedEncodedValue));

    final PreimageConditionContainer actualAddressContainer = objectMapper
        .readValue(json, PreimageConditionContainer.class);

    assertThat(actualAddressContainer).isEqualTo(expectedContainer);
    assertThat(actualAddressContainer.getCondition()).isEqualTo(CONDITION);
  }

  private static class PreimageConditionContainer {

    @JsonProperty("condition")
    private final InterledgerCondition condition;

    @JsonCreator
    private PreimageConditionContainer(
        @JsonProperty("condition") final InterledgerCondition condition
    ) {
      this.condition = Objects.requireNonNull(condition);
    }

    public InterledgerCondition getCondition() {
      return condition;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PreimageConditionContainer that = (PreimageConditionContainer) o;
      return Objects.equals(getCondition(), that.getCondition());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getCondition());
    }

    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer("PreimageConditionContainer{");
      sb.append("condition=").append(condition);
      sb.append('}');
      return sb.toString();
    }
  }


}
