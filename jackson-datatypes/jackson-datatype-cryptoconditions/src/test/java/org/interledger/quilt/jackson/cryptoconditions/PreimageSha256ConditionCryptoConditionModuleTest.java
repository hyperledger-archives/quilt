package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.PreimageSha256Condition;

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
public class PreimageSha256ConditionCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  private static final String PREIMAGE_CONDITION_DER_BYTES_HEX =
      "A0258020BF165845FFDB85F44A32052EC6279D2DBF151DE8E3A7D3727C94FC7AB531ACD581012B";
  private static final String PREIMAGE_CONDITION_DER_BYTES_BASE64
      = "oCWAIL8WWEX/24X0SjIFLsYnnS2/FR3o46fTcnyU/Hq1MazVgQEr";
  private static final String PREIMAGE_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oCWAIL8WWEX/24X0SjIFLsYnnS2/FR3o46fTcnyU/Hq1MazVgQEr";
  private static final String PREIMAGE_CONDITION_DER_BYTES_BASE64_URL
      = "oCWAIL8WWEX_24X0SjIFLsYnnS2_FR3o46fTcnyU_Hq1MazVgQEr";
  private static final String PREIMAGE_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oCWAIL8WWEX_24X0SjIFLsYnnS2_FR3o46fTcnyU_Hq1MazVgQEr";

  private static PreimageSha256Condition CONDITION = constructPreimageCondition();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public PreimageSha256ConditionCryptoConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    super(encodingToUse, expectedEncodedValue);
  }

  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleConditionTest constructor.
    return Arrays.asList(new Object[][]{
        {HEX, PREIMAGE_CONDITION_DER_BYTES_HEX},
        {BASE64, PREIMAGE_CONDITION_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, PREIMAGE_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, PREIMAGE_CONDITION_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, PREIMAGE_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING},
    });

  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final PreimageConditionContainer expectedContainer = ImmutablePreimageConditionContainer
        .builder()
        .condition(CONDITION)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"condition\":\"%s\"}", expectedEncodedValue)
    ));

    final PreimageConditionContainer actualAddressContainer = objectMapper
        .readValue(json, PreimageConditionContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getCondition(), is(CONDITION));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutablePreimageConditionContainer.class)
  @JsonDeserialize(as = ImmutablePreimageConditionContainer.class)
  interface PreimageConditionContainer {

    @JsonProperty("condition")
    PreimageSha256Condition getCondition();
  }

}