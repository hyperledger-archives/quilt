package org.interledger.quilt.jackson;

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

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.SharedSecret;
import org.interledger.quilt.jackson.conditions.Encoding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;

/**
 * Validates the functionality of {@link InterledgerModule}.
 */
@RunWith(Parameterized.class)
public class InterledgerModuleTest {

  private static final String CONDITION_BYTES_HEX =
      "D8391BB7F1E6AF13F9D21745481FF024AE8429A2280478B42B174D4716AC7353";
  private static final String CONDITION_BYTES_BASE64
      = "2Dkbt/HmrxP50hdFSB/wJK6EKaIoBHi0KxdNRxasc1M=";
  private static final String CONDITION_BYTES_BASE64_WITHOUTPADDING
      = "2Dkbt/HmrxP50hdFSB/wJK6EKaIoBHi0KxdNRxasc1M";
  private static final String CONDITION_BYTES_BASE64_URL
      = "2Dkbt_HmrxP50hdFSB_wJK6EKaIoBHi0KxdNRxasc1M=";
  private static final String CONDITION_BYTES_BASE64_URL_WITHOUTPADDING
      = "2Dkbt_HmrxP50hdFSB_wJK6EKaIoBHi0KxdNRxasc1M";

  private static InterledgerCondition CONDITION = constructCondition();

  private ObjectMapper objectMapper;
  private InterledgerCondition condition;
  private Encoding encodingToUse;
  private String expectedEncodedValue;

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   * @param condition            A {@link InterledgerCondition} to encode and decode for each test
   *                             run.
   */
  public InterledgerModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue,
      final InterledgerCondition condition
  ) {
    this.encodingToUse = Objects.requireNonNull(encodingToUse);
    this.expectedEncodedValue = Objects.requireNonNull(expectedEncodedValue);
    this.condition = Objects.requireNonNull(condition);
  }

  private static InterledgerCondition constructCondition() {
    final byte[] preimage = "you built a time machine out of ".getBytes();
    return InterledgerFulfillment.of(preimage).getCondition();
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
        {Encoding.HEX, CONDITION_BYTES_HEX, CONDITION},
        {Encoding.BASE64, CONDITION_BYTES_BASE64, CONDITION},
        {Encoding.BASE64_WITHOUT_PADDING, CONDITION_BYTES_BASE64_WITHOUTPADDING, CONDITION},
        {Encoding.BASE64URL, CONDITION_BYTES_BASE64_URL, CONDITION},
        {
            Encoding.BASE64URL_WITHOUT_PADDING,
            CONDITION_BYTES_BASE64_URL_WITHOUTPADDING,
            CONDITION
        }
    });
  }

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new InterledgerModule(encodingToUse));
  }

  @Test
  public void testSerializeDeserialize() throws IOException {

    final InterledgerAddress expectedAddress = InterledgerAddress.of("test1.ledger.foo");

    final InterledgerContainer expectedContainer
        = new InterledgerContainer(expectedAddress, condition);

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json).isEqualTo(
        String.format("{\"ledger_prefix\":\"%s\",\"execution_condition\":\"%s\"}",
            expectedContainer.getInterledgerAddress().getValue(),
            expectedEncodedValue)
    );

    final InterledgerContainer actualAddressContainer = objectMapper
        .readValue(json, InterledgerContainer.class);

    assertThat(actualAddressContainer).isEqualTo(expectedContainer);
    assertThat(actualAddressContainer.getCondition()).isEqualTo(condition);
  }

  @Test
  public void sharedSecretSerializeDeserialize() throws JsonProcessingException {
    byte[] key = new byte[32];
    new Random().nextBytes(key);
    SharedSecret sharedSecret = SharedSecret.of(key);

    String jsonValue = objectMapper.writeValueAsString(sharedSecret);
    SharedSecret fromJackson = objectMapper.readValue(jsonValue, SharedSecret.class);
    assertThat(fromJackson).isEqualTo(sharedSecret);
  }

  @Test
  public void sharedSecretBase64SerializeDeserialize() throws JsonProcessingException {
    byte[] key = new byte[32];
    new Random().nextBytes(key);
    String base64 = Base64.getEncoder().encodeToString(key);
    SharedSecret sharedSecret = SharedSecret.of(key);

    String jsonValue = objectMapper.writeValueAsString(sharedSecret);
    SharedSecret fromJackson = objectMapper.readValue(jsonValue, SharedSecret.class);
    assertThat(fromJackson).isEqualTo(sharedSecret);
  }

  private static class InterledgerContainer {

    @JsonProperty("ledger_prefix")
    private final InterledgerAddress interledgerAddress;

    @JsonProperty("execution_condition")
    private final InterledgerCondition condition;

    @JsonCreator
    public InterledgerContainer(
        @JsonProperty("ledger_prefix") final InterledgerAddress interledgerAddress,
        @JsonProperty("execution_condition") final InterledgerCondition condition
    ) {
      this.interledgerAddress = Objects.requireNonNull(interledgerAddress);
      this.condition = Objects.requireNonNull(condition);
    }

    public InterledgerAddress getInterledgerAddress() {
      return interledgerAddress;
    }

    public InterledgerCondition getCondition() {
      return condition;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      InterledgerContainer that = (InterledgerContainer) obj;

      if (!interledgerAddress.equals(that.interledgerAddress)) {
        return false;
      }
      return condition.equals(that.condition);
    }

    @Override
    public int hashCode() {
      int result = interledgerAddress.hashCode();
      result = 31 * result + condition.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "InterledgerContainer{"
          + "interledgerAddress=" + interledgerAddress
          + ", condition=" + condition
          + '}';
    }
  }
}
