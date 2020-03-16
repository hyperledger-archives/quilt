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
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.SharedSecret;
import org.interledger.link.LinkId;
import org.interledger.link.LinkType;
import org.interledger.quilt.jackson.conditions.Encoding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

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
   * @param condition            A {@link InterledgerCondition} to encode and decode for each test run.
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

    final InterledgerAddressPrefix expectedPrefix = InterledgerAddressPrefix.of("test1.ledger");

    final LinkId expectedLinkId = LinkId.of("linkId");
    final LinkType expectedLinkType = LinkType.of("linkType");
    final SharedSecret expectedSharedSecret = SharedSecret.of(new byte[32]);

    final InterledgerContainer expectedContainer = ImmutableInterledgerContainer.builder()
        .interledgerAddress(expectedAddress)
        .interledgerAddressPrefix(expectedPrefix)
        .condition(condition)
        .linkId(expectedLinkId)
        .linkType(expectedLinkType)
        .sharedSecret(expectedSharedSecret)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json).isEqualTo(
        String.format("{\"ledger_address\":\"%s\",\"ledger_prefix\":\"%s\",\"execution_condition\":\"%s\","
                + "\"shared_secret\":\"%s\",\"link_id\":\"%s\",\"link_type\":\"%s\"}",
            expectedContainer.getInterledgerAddress().getValue(),
            expectedContainer.getInterledgerAddressPrefix().getValue(),
            expectedEncodedValue,
            expectedSharedSecret.value(),
            expectedLinkId.value(),
            expectedLinkType.value()
        )
    );

    final InterledgerContainer actualAddressContainer = objectMapper
        .readValue(json, InterledgerContainer.class);

    assertThat(actualAddressContainer).isEqualTo(expectedContainer);
    assertThat(actualAddressContainer.getInterledgerAddressPrefix()).isEqualTo(expectedPrefix);
    assertThat(actualAddressContainer.getCondition()).isEqualTo(condition);
    assertThat(actualAddressContainer.getLinkId()).isEqualTo(expectedLinkId);
    assertThat(actualAddressContainer.getLinkType()).isEqualTo(expectedLinkType);
    assertThat(actualAddressContainer.getSharedSecret()).isEqualTo(expectedSharedSecret);
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableInterledgerContainer.class)
  @JsonDeserialize(as = ImmutableInterledgerContainer.class)
  interface InterledgerContainer {

    @JsonProperty("ledger_address")
    InterledgerAddress getInterledgerAddress();

    @JsonProperty("ledger_prefix")
    InterledgerAddressPrefix getInterledgerAddressPrefix();

    @JsonProperty("execution_condition")
    InterledgerCondition getCondition();

    @JsonProperty("shared_secret")
    SharedSecret getSharedSecret();

    @JsonProperty("link_id")
    LinkId getLinkId();

    @JsonProperty("link_type")
    LinkType getLinkType();
  }
}
