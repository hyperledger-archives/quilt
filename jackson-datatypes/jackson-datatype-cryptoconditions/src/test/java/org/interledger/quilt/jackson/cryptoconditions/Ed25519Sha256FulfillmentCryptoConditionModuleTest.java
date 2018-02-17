package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64URL_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.BASE64_WITHOUT_PADDING;
import static org.interledger.quilt.jackson.cryptoconditions.Encoding.HEX;

import org.interledger.cryptoconditions.Ed25519Sha256Fulfillment;

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
public class Ed25519Sha256FulfillmentCryptoConditionModuleTest extends
    AbstractCryptoConditionsModuleTest {

  private static final String ED25519_FULFILLMENT_DER_BYTES_HEX =
      "A47A802036AE1B97C577AE6AFB0294E91839FA7B1F9332791B9F2C5D586819025F4A2F1D815635565A44414D4E67"
          + "72484B5168754C4D67473643696F53486678363435646C30324850675A534A4A41565666754949566B4B4D"
          + "37724D59654F5841632D625272306C763138466C627669526C555546446A6E6F514377";
  private static final String ED25519_FULFILLMENT_DER_BYTES_BASE64
      = "pHqAIDauG5fFd65q+wKU6Rg5+nsfkzJ5G58sXVhoGQJfSi8dgVY1VlpEQU1OZ3JIS1FodUxNZ0c2Q2lvU0hmeDY0NW"
      + "RsMDJIUGdaU0pKQVZWZnVJSVZrS003ck1ZZU9YQWMtYlJyMGx2MThGbGJ2aVJsVVVGRGpub1FDdw==";
  private static final String ED25519_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "pHqAIDauG5fFd65q+wKU6Rg5+nsfkzJ5G58sXVhoGQJfSi8dgVY1VlpEQU1OZ3JIS1FodUxNZ0c2Q2lvU0hmeDY0NW"
      + "RsMDJIUGdaU0pKQVZWZnVJSVZrS003ck1ZZU9YQWMtYlJyMGx2MThGbGJ2aVJsVVVGRGpub1FDdw";
  private static final String ED25519_FULFILLMENT_DER_BYTES_BASE64_URL
      = "pHqAIDauG5fFd65q-wKU6Rg5-nsfkzJ5G58sXVhoGQJfSi8dgVY1VlpEQU1OZ3JIS1FodUxNZ0c2Q2lvU0hmeDY0NW"
      + "RsMDJIUGdaU0pKQVZWZnVJSVZrS003ck1ZZU9YQWMtYlJyMGx2MThGbGJ2aVJsVVVGRGpub1FDdw==";
  private static final String ED25519_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "pHqAIDauG5fFd65q-wKU6Rg5-nsfkzJ5G58sXVhoGQJfSi8dgVY1VlpEQU1OZ3JIS1FodUxNZ0c2Q2lvU0hmeDY0NW"
      + "RsMDJIUGdaU0pKQVZWZnVJSVZrS003ck1ZZU9YQWMtYlJyMGx2MThGbGJ2aVJsVVVGRGpub1FDdw";

  private static Ed25519Sha256Fulfillment FULFILLMENT = constructEd25519Fulfillment();

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public Ed25519Sha256FulfillmentCryptoConditionModuleTest(
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
        {HEX, ED25519_FULFILLMENT_DER_BYTES_HEX},
        {BASE64, ED25519_FULFILLMENT_DER_BYTES_BASE64},
        {BASE64_WITHOUT_PADDING, ED25519_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING},
        {BASE64URL, ED25519_FULFILLMENT_DER_BYTES_BASE64_URL},
        {BASE64URL_WITHOUT_PADDING, ED25519_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING}
    });

  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final Ed25519FulfillmentContainer expectedContainer = ImmutableEd25519FulfillmentContainer
        .builder()
        .fulfillment(FULFILLMENT)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"fulfillment\":\"%s\"}", expectedEncodedValue)
    ));

    final Ed25519FulfillmentContainer actualAddressContainer = objectMapper
        .readValue(json, Ed25519FulfillmentContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getFulfillment(), is(FULFILLMENT));
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableEd25519FulfillmentContainer.class)
  @JsonDeserialize(as = ImmutableEd25519FulfillmentContainer.class)
  interface Ed25519FulfillmentContainer {

    @JsonProperty("fulfillment")
    Ed25519Sha256Fulfillment getFulfillment();
  }
}