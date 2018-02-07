package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.cryptoconditions.Condition;

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
public class GenericConditionCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  private static final String CONDITION_DER_BYTES_HEX =
      "A0258020BF165845FFDB85F44A32052EC6279D2DBF151DE8E3A7D3727C94FC7AB531ACD581012B";
  private static final String CONDITION_DER_BYTES_BASE64
      = "oCWAIL8WWEX/24X0SjIFLsYnnS2/FR3o46fTcnyU/Hq1MazVgQEr";
  private static final String CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oCWAIL8WWEX/24X0SjIFLsYnnS2/FR3o46fTcnyU/Hq1MazVgQEr";
  private static final String CONDITION_DER_BYTES_BASE64_URL
      = "oCWAIL8WWEX_24X0SjIFLsYnnS2_FR3o46fTcnyU_Hq1MazVgQEr";
  private static final String CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oCWAIL8WWEX_24X0SjIFLsYnnS2_FR3o46fTcnyU_Hq1MazVgQEr";

  private static Condition CONDITION = constructPreimageCondition();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public GenericConditionCryptoConditionModuleTest(
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
        {Encoding.HEX, CONDITION_DER_BYTES_HEX},
        {Encoding.BASE64, CONDITION_DER_BYTES_BASE64},
        {Encoding.BASE64_WITHOUT_PADDING, CONDITION_DER_BYTES_BASE64_WITHOUTPADDING},
        {Encoding.BASE64URL, CONDITION_DER_BYTES_BASE64_URL},
        {Encoding.BASE64URL_WITHOUT_PADDING, CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING},
    });
  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final ConditionContainer expectedContainer
        = ImmutableConditionContainer.builder().condition(CONDITION).build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"condition\":\"%s\"}", expectedEncodedValue)
    ));

    final ConditionContainer actualAddressContainer = objectMapper
        .readValue(json, ConditionContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getCondition(), is(CONDITION));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableConditionContainer.class)
  @JsonDeserialize(as = ImmutableConditionContainer.class)
  interface ConditionContainer {

    @JsonProperty("condition")
    Condition getCondition();
  }

}