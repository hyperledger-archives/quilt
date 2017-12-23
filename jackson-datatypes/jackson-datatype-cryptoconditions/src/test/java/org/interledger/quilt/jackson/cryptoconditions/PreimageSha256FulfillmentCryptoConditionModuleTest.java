package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.PreimageSha256Fulfillment;

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
public class PreimageSha256FulfillmentCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_HEX =
      "A02D802B796F75206275696C7420612074696D65206D616368696E65206F7574206F6620612044654C6F7265616E3F";
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_BASE64
      = "oC2AK3lvdSBidWlsdCBhIHRpbWUgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8=";
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "oC2AK3lvdSBidWlsdCBhIHRpbWUgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8";
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL
      = "oC2AK3lvdSBidWlsdCBhIHRpbWUgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8=";
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oC2AK3lvdSBidWlsdCBhIHRpbWUgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8";

  private static PreimageSha256Fulfillment FULFILLMENT = constructPreimageFulfillment();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public PreimageSha256FulfillmentCryptoConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    super(encodingToUse, expectedEncodedValue);
  }

  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleFulfillmentTest constructor.
    return Arrays.asList(new Object[][]{
        {HEX, PREIMAGE_FULFILLMENT_DER_BYTES_HEX},
        {BASE64, PREIMAGE_FULFILLMENT_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING},
    });

  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final PreimageFulfillmentContainer expectedContainer = ImmutablePreimageFulfillmentContainer
        .builder()
        .fulfillment(FULFILLMENT)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"fulfillment\":\"%s\"}", expectedEncodedValue)
    ));

    final PreimageFulfillmentContainer actualAddressContainer = objectMapper
        .readValue(json, PreimageFulfillmentContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getFulfillment(), is(FULFILLMENT));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutablePreimageFulfillmentContainer.class)
  @JsonDeserialize(as = ImmutablePreimageFulfillmentContainer.class)
  interface PreimageFulfillmentContainer {

    @JsonProperty("fulfillment")
    PreimageSha256Fulfillment getFulfillment();
  }

}