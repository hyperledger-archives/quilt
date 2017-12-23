package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.Ed25519Sha256Condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Validates the functionality of {@link CryptoConditionsModule}.
 */
@RunWith(Parameterized.class)
public class Ed25519Sha256ConditionCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  private static final String ED25519_CONDITION_DER_BYTES_HEX =
      "A4278020689E64935CE7DAAAD040EE50858657A068A0BF4639AD269F895DC150CD45F6138103020000";
  private static final String ED25519_CONDITION_DER_BYTES_BASE64
      = "pCeAIGieZJNc59qq0EDuUIWGV6BooL9GOa0mn4ldwVDNRfYTgQMCAAA=";
  private static final String ED25519_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "pCeAIGieZJNc59qq0EDuUIWGV6BooL9GOa0mn4ldwVDNRfYTgQMCAAA";
  private static final String ED25519_CONDITION_DER_BYTES_BASE64_URL
      = "pCeAIGieZJNc59qq0EDuUIWGV6BooL9GOa0mn4ldwVDNRfYTgQMCAAA=";
  private static final String ED25519_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "pCeAIGieZJNc59qq0EDuUIWGV6BooL9GOa0mn4ldwVDNRfYTgQMCAAA";

  private static Ed25519Sha256Condition CONDITION = constructEd25519Condition();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public Ed25519Sha256ConditionCryptoConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    super(encodingToUse, expectedEncodedValue);
  }

  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleConditionTest constructor.
    return Arrays.asList(new Object[][]{
        {HEX, ED25519_CONDITION_DER_BYTES_HEX},
        {BASE64, ED25519_CONDITION_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, ED25519_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, ED25519_CONDITION_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, ED25519_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING}
    });

  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final Ed25519ConditionContainer expectedContainer = ImmutableEd25519ConditionContainer.builder()
        .condition(CONDITION)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"condition\":\"%s\"}", expectedEncodedValue)
    ));

    final Ed25519ConditionContainer actualAddressContainer = objectMapper
        .readValue(json, Ed25519ConditionContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getCondition(), is(CONDITION));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableEd25519ConditionContainer.class)
  @JsonDeserialize(as = ImmutableEd25519ConditionContainer.class)
  interface Ed25519ConditionContainer {

    @JsonProperty("condition")
    Ed25519Sha256Condition getCondition();
  }
}