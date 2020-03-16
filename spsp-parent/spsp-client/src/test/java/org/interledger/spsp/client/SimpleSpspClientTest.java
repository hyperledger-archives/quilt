package org.interledger.spsp.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SimpleSpspClientTest {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
  private SpspClient client;

  @Before
  public void setUp() {
    client = new SimpleSpspClient(SpspClientDefaults.OK_HTTP_CLIENT,
        (pointer) -> HttpUrl.parse(wireMockRule.baseUrl() + pointer.path()),
        SpspClientDefaults.MAPPER);
  }

  @Test
  public void getStreamConnectionDetailsFromPaymentPointer() {
    String testClient = "testClient";
    String sharedSecret = "Nf9wCXI1OZLM/QIWdZZ2Q39limh6+Yxhm/FB1bUpZLA=";

    String wiremockIlpAddress = "test.wiremock";
    stubFor(get(urlEqualTo("/" + testClient))
        .withHeader("Accept", equalTo(SpspClient.ACCEPT_APPLICATION_SPSP4_JSON))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"destination_account\":\"" + wiremockIlpAddress + "." + testClient + "\","
                + "\"shared_secret\":\"" + sharedSecret + "\"}")
        ));

    StreamConnectionDetails response =
        client.getStreamConnectionDetails(paymentPointer(testClient));

    assertThat(response)
        .isEqualTo(StreamConnectionDetails.builder()
            .destinationAddress(InterledgerAddress.of(wiremockIlpAddress + "." + testClient))
            .sharedSecret(SharedSecret.of(sharedSecret))
            .build());
  }

  @Test
  public void getStreamConnectionDetailsFromUrl() {
    String testClient = "testClient";
    String sharedSecret = "Nf9wCXI1OZLM/QIWdZZ2Q39limh6+Yxhm/FB1bUpZLA=";

    String wiremockIlpAddress = "test.wiremock";
    stubFor(get(urlEqualTo("/" + testClient))
        .withHeader("Accept", equalTo(SpspClient.ACCEPT_APPLICATION_SPSP4_JSON))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"destination_account\":\"" + wiremockIlpAddress + "." + testClient + "\","
                + "\"shared_secret\":\"" + sharedSecret + "\"}")
        ));

    HttpUrl url = HttpUrl.parse(wireMockRule.baseUrl() + "/" + testClient);
    StreamConnectionDetails response = client.getStreamConnectionDetails(url);

    assertThat(response)
        .isEqualTo(StreamConnectionDetails.builder()
            .destinationAddress(InterledgerAddress.of(wiremockIlpAddress + "." + testClient))
            .sharedSecret(SharedSecret.of(sharedSecret))
            .build());
  }

  @Test(expected = InvalidReceiverClientException.class)
  public void getStreamConnectionDetails404ThrowsInvalidReceiver() {
    String testClient = "testClient";

    stubFor(get(urlEqualTo("/" + testClient))
        .withHeader("Accept", equalTo(SpspClient.ACCEPT_APPLICATION_SPSP4_JSON))
        .willReturn(aResponse()
            .withStatus(404)
        ));

    client.getStreamConnectionDetails(paymentPointer(testClient));
  }

  @Test(expected = SpspClientException.class)
  public void getStreamConnectionDetails500ThrowsSpspClientException() {
    String testClient = "testClient";

    stubFor(get(urlEqualTo("/" + testClient))
        .withHeader("Accept", equalTo(SpspClient.ACCEPT_APPLICATION_SPSP4_JSON))
        .willReturn(aResponse()
            .withStatus(503)
        ));

    client.getStreamConnectionDetails(paymentPointer(testClient));
  }

  @Test
  public void testHeaders() {
    assertThat(SpspClient.APPLICATION_SPSP4_JSON).isEqualTo("application/spsp4+json");
    assertThat(SpspClient.ACCEPT_APPLICATION_SPSP4_JSON).isEqualTo("application/spsp4+json, application/spsp+json");
  }

  private PaymentPointer paymentPointer(String testClient) {
    return PaymentPointer.of("$test.wiremock/" + testClient);
  }

}
