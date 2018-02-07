package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.RsaSha256Condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

/**
 * Validates the functionality of {@link CryptoConditionsModule}.
 */
@RunWith(Parameterized.class)
public class RsaSha256ConditionCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);

  // RSA
  private static final String RSA_CONDITION_DER_BYTES_HEX =
      "A3268020C92FD89F3EBEEE47E69E20EE14521E9E5605ADC734F858EBD1C35F2025DA5D1D81024000";
  private static final String RSA_CONDITION_DER_BYTES_BASE64
      = "oyaAIMkv2J8+vu5H5p4g7hRSHp5WBa3HNPhY69HDXyAl2l0dgQJAAA==";
  private static final String RSA_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oyaAIMkv2J8+vu5H5p4g7hRSHp5WBa3HNPhY69HDXyAl2l0dgQJAAA";
  private static final String RSA_CONDITION_DER_BYTES_BASE64_URL
      = "oyaAIMkv2J8-vu5H5p4g7hRSHp5WBa3HNPhY69HDXyAl2l0dgQJAAA==";
  private static final String RSA_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oyaAIMkv2J8-vu5H5p4g7hRSHp5WBa3HNPhY69HDXyAl2l0dgQJAAA";

  private static RsaSha256Condition CONDITION = constructRsaCondition();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public RsaSha256ConditionCryptoConditionModuleTest(
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
        {HEX, RSA_CONDITION_DER_BYTES_HEX},
        {BASE64, RSA_CONDITION_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, RSA_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, RSA_CONDITION_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, RSA_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING}
    });
  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final RsaConditionContainer expectedContainer = ImmutableRsaConditionContainer.builder()
        .condition(CONDITION)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"condition\":\"%s\"}", expectedEncodedValue)
    ));

    final RsaConditionContainer actualAddressContainer = objectMapper
        .readValue(json, RsaConditionContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getCondition(), is(CONDITION));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableRsaConditionContainer.class)
  @JsonDeserialize(as = ImmutableRsaConditionContainer.class)
  interface RsaConditionContainer {

    @JsonProperty("condition")
    RsaSha256Condition getCondition();
  }


}