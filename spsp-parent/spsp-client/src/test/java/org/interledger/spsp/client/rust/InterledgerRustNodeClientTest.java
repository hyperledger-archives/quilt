package org.interledger.spsp.client.rust;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

public class InterledgerRustNodeClientTest {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(0);
  private InterledgerRustNodeClient client;

  @Before
  public void setup() {
    client = new InterledgerRustNodeClient(new OkHttpClient(), "passwordin", wireMockRule.baseUrl());
  }

  @Test
  public void createAccount() throws IOException, URISyntaxException {
    String testClient = "testClient";
    String accountBody = "{\"ilp_address\":\"test.xpring-dev.rs1." + testClient + "\","
        + "\"username\":\"" + testClient + "\","
        + "\"asset_code\":\"XRP\","
        + "\"asset_scale\":6,"
        + "\"max_packet_amount\":10000000,"
        + "\"min_balance\":-100000000,"
        + "\"ilp_over_http_incoming_token\":\"passwordin\","
        + "\"ilp_over_http_outgoing_token\":\"passwordout\","
        + "\"http_endpoint\":\"https://foo.com\","
        + "\"btp_uri\":\"btp+ws://foo.com:8000\","
        + "\"routing_relation\":\"Peer\","
        + "\"round_trip_time\":500}";

    stubFor(post(urlEqualTo("/accounts"))
        .withHeader("Authorization", equalTo("Bearer passwordin"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(accountBody)
        ));

    client.createAccount(RustNodeAccount.builder()
        .ilpAddress(InterledgerAddress.of("test.xpring-dev.rs1").with(testClient))
        .username(testClient)
        .assetCode("XRP")
        .assetScale(6)
        .minBalance(new BigInteger("-100000000"))
        .maxPacketAmount(new BigInteger("10000000"))
        .httpIncomingToken("passwordin")
        .httpOutgoingToken("passwordout")
        .httpEndpoint(new URI("https://foo.com"))
        .btpUri(new URI("btp+ws://foo.com:8000"))
        .roundTripTime(new BigInteger("500"))
        .routingRelation(RustNodeAccount.RoutingRelation.PEER)
        .build()
    );

    verify(postRequestedFor(urlMatching("/accounts"))
        .withRequestBody(equalTo(accountBody)));
  }

  @Test
  public void getBalance() {
    String bigBalance = "12345678901234567890.11111";
    String testClient = "testClient";

    stubFor(get(urlEqualTo("/accounts/" + testClient + "/balance"))
        .withHeader("Authorization", equalTo("Bearer passwordin"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"balance\":\"" + bigBalance + "\"}")
        ));

    BigDecimal response = client.getBalance(testClient);

    assertThat(response).isEqualTo(new BigDecimal(bigBalance));
  }
}
