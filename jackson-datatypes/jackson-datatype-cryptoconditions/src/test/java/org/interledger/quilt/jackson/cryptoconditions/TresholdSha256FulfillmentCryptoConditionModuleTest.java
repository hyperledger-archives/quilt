package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.ThresholdSha256Fulfillment;

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
public class TresholdSha256FulfillmentCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_HEX =
      "A2820125A082011FA02D802B796F75206275696C7420612074696D65206D616368696E65206F7574206F66206120"
          + "44654C6F7265616E3FA3818E808180A56E4A0E701017589A5187DC7EA841D156F2EC0E36AD52A44DFEB1E6"
          + "1F7AD991D8C51056FFEDB162B4C0F283A12A88A394DFF526AB7291CBB307CEABFCE0B1DFD5CD9508096D5B"
          + "2B8B6DF5D671EF6377C0921CB23C270A70E2598E6FF89D19F105ACC2D3F0CB35F29280E1386B6F64C4EF22"
          + "E1E1F20D0CE8CFFB2249BD9A213781097369676E6174757265A15D802749276D20796F75722064656E7369"
          + "74792E2049206D65616E2C20796F75722064657374696E792E810114A22FA02D802B796F75206275696C74"
          + "20612074696D65206D616368696E65206F7574206F6620612044654C6F7265616E3FA100";
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_BASE64
      = "ooIBJaCCAR+gLYAreW91IGJ1aWx0IGEgdGltZSBtYWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6OBjoCBgKVuSg5wEB"
      + "dYmlGH3H6oQdFW8uwONq1SpE3+seYfetmR2MUQVv/tsWK0wPKDoSqIo5Tf9SarcpHLswfOq/zgsd/VzZUICW1bK4tt"
      + "9dZx72N3wJIcsjwnCnDiWY5v+J0Z8QWswtPwyzXykoDhOGtvZMTvIuHh8g0M6M/7Ikm9miE3gQlzaWduYXR1cmWhXY"
      + "AnSSdtIHlvdXIgZGVuc2l0eS4gSSBtZWFuLCB5b3VyIGRlc3RpbnkugQEUoi+gLYAreW91IGJ1aWx0IGEgdGltZSBt"
      + "YWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6EA";
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "ooIBJaCCAR+gLYAreW91IGJ1aWx0IGEgdGltZSBtYWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6OBjoCBgKVuSg5wEB"
      + "dYmlGH3H6oQdFW8uwONq1SpE3+seYfetmR2MUQVv/tsWK0wPKDoSqIo5Tf9SarcpHLswfOq/zgsd/VzZUICW1bK4tt"
      + "9dZx72N3wJIcsjwnCnDiWY5v+J0Z8QWswtPwyzXykoDhOGtvZMTvIuHh8g0M6M/7Ikm9miE3gQlzaWduYXR1cmWhXY"
      + "AnSSdtIHlvdXIgZGVuc2l0eS4gSSBtZWFuLCB5b3VyIGRlc3RpbnkugQEUoi+gLYAreW91IGJ1aWx0IGEgdGltZSBt"
      + "YWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6EA";
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_URL
      = "ooIBJaCCAR-gLYAreW91IGJ1aWx0IGEgdGltZSBtYWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6OBjoCBgKVuSg5wEB"
      + "dYmlGH3H6oQdFW8uwONq1SpE3-seYfetmR2MUQVv_tsWK0wPKDoSqIo5Tf9SarcpHLswfOq_zgsd_VzZUICW1bK4tt"
      + "9dZx72N3wJIcsjwnCnDiWY5v-J0Z8QWswtPwyzXykoDhOGtvZMTvIuHh8g0M6M_7Ikm9miE3gQlzaWduYXR1cmWhXY"
      + "AnSSdtIHlvdXIgZGVuc2l0eS4gSSBtZWFuLCB5b3VyIGRlc3RpbnkugQEUoi-gLYAreW91IGJ1aWx0IGEgdGltZSBt"
      + "YWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6EA";
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "ooIBJaCCAR-gLYAreW91IGJ1aWx0IGEgdGltZSBtYWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6OBjoCBgKVuSg5wEB"
      + "dYmlGH3H6oQdFW8uwONq1SpE3-seYfetmR2MUQVv_tsWK0wPKDoSqIo5Tf9SarcpHLswfOq_zgsd_VzZUICW1bK4tt"
      + "9dZx72N3wJIcsjwnCnDiWY5v-J0Z8QWswtPwyzXykoDhOGtvZMTvIuHh8g0M6M_7Ikm9miE3gQlzaWduYXR1cmWhXY"
      + "AnSSdtIHlvdXIgZGVuc2l0eS4gSSBtZWFuLCB5b3VyIGRlc3RpbnkugQEUoi-gLYAreW91IGJ1aWx0IGEgdGltZSBt"
      + "YWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6EA";

  private static ThresholdSha256Fulfillment FULFILLMENT = constructThresholdFulfillment();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public TresholdSha256FulfillmentCryptoConditionModuleTest(
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
    // to the CryptoConditionsModuleFulfillmentTest constructor.
    return Arrays.asList(new Object[][]{
        {HEX, THRESHOLD_FULFILLMENT_DER_BYTES_HEX},
        {BASE64, THRESHOLD_FULFILLMENT_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING}
    });
  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final ThresholdFulfillmentContainer expectedContainer = ImmutableThresholdFulfillmentContainer
        .builder()
        .fulfillment(FULFILLMENT)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"fulfillment\":\"%s\"}", expectedEncodedValue)
    ));

    final ThresholdFulfillmentContainer actualAddressContainer = objectMapper
        .readValue(json, ThresholdFulfillmentContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getFulfillment(), is(FULFILLMENT));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableThresholdFulfillmentContainer.class)
  @JsonDeserialize(as = ImmutableThresholdFulfillmentContainer.class)
  interface ThresholdFulfillmentContainer {

    @JsonProperty("fulfillment")
    ThresholdSha256Fulfillment getFulfillment();
  }

}