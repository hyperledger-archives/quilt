package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.ThresholdSha256Condition;

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
public class TresholdSha256ConditionCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  private static final String THRESHOLD_CONDITION_DER_BYTES_HEX =
      "A22A8020ECF2CD7971471204029D36833A1D548D3AB476B8957876B7494D8058A0AE4E6C81025066820204D0";
  private static final String THRESHOLD_CONDITION_DER_BYTES_BASE64
      = "oiqAIOzyzXlxRxIEAp02gzodVI06tHa4lXh2t0lNgFigrk5sgQJQZoICBNA=";
  private static final String THRESHOLD_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oiqAIOzyzXlxRxIEAp02gzodVI06tHa4lXh2t0lNgFigrk5sgQJQZoICBNA";
  private static final String THRESHOLD_CONDITION_DER_BYTES_BASE64_URL
      = "oiqAIOzyzXlxRxIEAp02gzodVI06tHa4lXh2t0lNgFigrk5sgQJQZoICBNA=";
  private static final String THRESHOLD_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oiqAIOzyzXlxRxIEAp02gzodVI06tHa4lXh2t0lNgFigrk5sgQJQZoICBNA";

  private static ThresholdSha256Condition CONDITION = constructThresholdCondition();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public TresholdSha256ConditionCryptoConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    super(encodingToUse, expectedEncodedValue);
  }

  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleConditionTest constructor.
    return Arrays.asList(new Object[][]{
        {HEX, THRESHOLD_CONDITION_DER_BYTES_HEX},
        {BASE64, THRESHOLD_CONDITION_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, THRESHOLD_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, THRESHOLD_CONDITION_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, THRESHOLD_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING}
    });
  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final ThresholdConditionContainer expectedContainer = ImmutableThresholdConditionContainer
        .builder()
        .condition(CONDITION)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"condition\":\"%s\"}", expectedEncodedValue)
    ));

    final ThresholdConditionContainer actualAddressContainer = objectMapper
        .readValue(json, ThresholdConditionContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getCondition(), is(CONDITION));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableThresholdConditionContainer.class)
  @JsonDeserialize(as = ImmutableThresholdConditionContainer.class)
  interface ThresholdConditionContainer {

    @JsonProperty("condition")
    ThresholdSha256Condition getCondition();
  }

}