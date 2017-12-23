package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.RsaSha256Fulfillment;

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
public class RsaSha256FulfillmentCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);

  private static final String RSA_FULFILLMENT_DER_BYTES_HEX =
      "A3818E808180A56E4A0E701017589A5187DC7EA841D156F2EC0E36AD52A44DFEB1E61F7AD991D8C51056FFEDB162"
          + "B4C0F283A12A88A394DFF526AB7291CBB307CEABFCE0B1DFD5CD9508096D5B2B8B6DF5D671EF6377C0921C"
          + "B23C270A70E2598E6FF89D19F105ACC2D3F0CB35F29280E1386B6F64C4EF22E1E1F20D0CE8CFFB2249BD9A"
          + "213781097369676E6174757265";
  private static final String RSA_FULFILLMENT_DER_BYTES_BASE64
      = "o4GOgIGApW5KDnAQF1iaUYfcfqhB0Vby7A42rVKkTf6x5h962ZHYxRBW/+2xYrTA8oOhKoijlN/1JqtykcuzB86r/O"
      + "Cx39XNlQgJbVsri2311nHvY3fAkhyyPCcKcOJZjm/4nRnxBazC0/DLNfKSgOE4a29kxO8i4eHyDQzoz/siSb2aITeB"
      + "CXNpZ25hdHVyZQ==";
  private static final String RSA_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "o4GOgIGApW5KDnAQF1iaUYfcfqhB0Vby7A42rVKkTf6x5h962ZHYxRBW/+2xYrTA8oOhKoijlN/1JqtykcuzB86r/O"
      + "Cx39XNlQgJbVsri2311nHvY3fAkhyyPCcKcOJZjm/4nRnxBazC0/DLNfKSgOE4a29kxO8i4eHyDQzoz/siSb2aITeB"
      + "CXNpZ25hdHVyZQ";
  private static final String RSA_FULFILLMENT_DER_BYTES_BASE64_URL
      = "o4GOgIGApW5KDnAQF1iaUYfcfqhB0Vby7A42rVKkTf6x5h962ZHYxRBW_-2xYrTA8oOhKoijlN_1JqtykcuzB86r_O"
      + "Cx39XNlQgJbVsri2311nHvY3fAkhyyPCcKcOJZjm_4nRnxBazC0_DLNfKSgOE4a29kxO8i4eHyDQzoz_siSb2aITeB"
      + "CXNpZ25hdHVyZQ==";
  private static final String RSA_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "o4GOgIGApW5KDnAQF1iaUYfcfqhB0Vby7A42rVKkTf6x5h962ZHYxRBW_-2xYrTA8oOhKoijlN_1JqtykcuzB86r_O"
      + "Cx39XNlQgJbVsri2311nHvY3fAkhyyPCcKcOJZjm_4nRnxBazC0_DLNfKSgOE4a29kxO8i4eHyDQzoz_siSb2aITeB"
      + "CXNpZ25hdHVyZQ";

  private static RsaSha256Fulfillment FULFILLMENT = constructRsaFulfillment();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public RsaSha256FulfillmentCryptoConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    super(encodingToUse, expectedEncodedValue);
  }

  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleFulfillmentTest constructor.
    return Arrays.asList(new Object[][]{
        {HEX, RSA_FULFILLMENT_DER_BYTES_HEX},
        {BASE64, RSA_FULFILLMENT_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, RSA_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, RSA_FULFILLMENT_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, RSA_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING}
    });
  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final RsaFulfillmentContainer expectedContainer = ImmutableRsaFulfillmentContainer.builder()
        .fulfillment(FULFILLMENT)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"fulfillment\":\"%s\"}", expectedEncodedValue)
    ));

    final RsaFulfillmentContainer actualAddressContainer = objectMapper
        .readValue(json, RsaFulfillmentContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getFulfillment(), is(FULFILLMENT));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableRsaFulfillmentContainer.class)
  @JsonDeserialize(as = ImmutableRsaFulfillmentContainer.class)
  interface RsaFulfillmentContainer {

    @JsonProperty("fulfillment")
    RsaSha256Fulfillment getFulfillment();
  }


}