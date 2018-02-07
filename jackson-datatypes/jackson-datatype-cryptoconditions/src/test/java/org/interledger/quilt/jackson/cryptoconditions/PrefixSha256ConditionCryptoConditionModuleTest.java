package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.PrefixSha256Condition;

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
public class PrefixSha256ConditionCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  private static final String PREFIX_CONDITION_DER_BYTES_HEX =
      "A12A8020E9241D95AD4FC17A373E609BB69FF0263C83597DD6BC758F162BF0D9D15F09EA8102046682020780";
  private static final String PREFIX_CONDITION_DER_BYTES_BASE64
      = "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A=";
  private static final String PREFIX_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A";
  private static final String PREFIX_CONDITION_DER_BYTES_BASE64_URL
      = "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A=";
  private static final String PREFIX_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A";

  private static PrefixSha256Condition CONDITION = constructPrefixCondition();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public PrefixSha256ConditionCryptoConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    super(encodingToUse, expectedEncodedValue);
  }

  /**
   * Get test parameters.
   * @return the parameters for the tests
   */
  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleConditionTest constructor.
    return Arrays.asList(new Object[][]{
        {HEX, PREFIX_CONDITION_DER_BYTES_HEX},
        {BASE64, PREFIX_CONDITION_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, PREFIX_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, PREFIX_CONDITION_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, PREFIX_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING},
    });

  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final PrefixConditionContainer expectedContainer = ImmutablePrefixConditionContainer.builder()
        .condition(CONDITION)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"condition\":\"%s\"}", expectedEncodedValue)
    ));

    final PrefixConditionContainer actualAddressContainer = objectMapper
        .readValue(json, PrefixConditionContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getCondition(), is(CONDITION));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutablePrefixConditionContainer.class)
  @JsonDeserialize(as = ImmutablePrefixConditionContainer.class)
  interface PrefixConditionContainer {

    @JsonProperty("condition")
    PrefixSha256Condition getCondition();
  }

}