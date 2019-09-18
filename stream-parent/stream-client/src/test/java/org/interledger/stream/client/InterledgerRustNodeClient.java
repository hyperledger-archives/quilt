package org.interledger.stream.client;

import static org.assertj.core.api.Assertions.assertThat;
import org.interledger.stream.ImmutableStreamConnectionDetails;
import org.interledger.stream.StreamConnectionDetails;
import java.io.IOException;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.immutables.value.Value.Immutable;

public class InterledgerRustNodeClient {

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient httpClient;
  private String authToken;
  private ObjectMapper objectMapper;
  private String baseUri;

  public InterledgerRustNodeClient(OkHttpClient okHttpClient, String authToken, ObjectMapper objectMapper, String baseUri) {
    this.httpClient = okHttpClient;
    this.authToken = authToken;
    this.objectMapper = objectMapper;
    this.baseUri = baseUri;
  }

  public void createAccount(String ilpAddress, String accountName) throws IOException {
    ImmutableMap<Object, Object> newAccountPayload = ImmutableMap.builder()
        .put("ilp_address", ilpAddress + "." + accountName)
        .put("username", accountName)
        .put("asset_code", "XRP")
        .put("asset_scale", 6)
        .put("http_endpoint", "https://peer-ilp-over-http-endpoint.example/ilp")
        .put("http_incoming_token", authToken)
        .put("http_outgoing_token", authToken)
        .put("is_admin", false)
        .put("min_balance", -1000000000L)
        .put("receive_routes", false)
        .put("send_routes", false)
        .put("round_trip_times", 500)
        .put("routing_relation", "Peer")
        .build();

    Request request = new Request.Builder()
        .url(HttpUrl.parse(baseUri + "/accounts"))
        .post(RequestBody.create(new ObjectMapper().writeValueAsString(newAccountPayload), JSON))
        .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)))
        .build();
    try (Response response = httpClient.newCall(request).execute()) {
      assertThat(response.code()).isEqualTo(200);
    }
  }

  public StreamConnectionDetails getStreamConnectionDetails(String account) throws IOException {
    Request request = new Request.Builder()
        .url(HttpUrl.parse(baseUri + "/spsp/" + account))
        .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)))
        .get()
        .build();
    try (Response response = httpClient.newCall(request).execute()) {
      assertThat(response.code()).isEqualTo(200);
      return objectMapper.readValue(response.body().string(), ImmutableStreamConnectionDetails.class);
    }
  }


  public BigDecimal getBalance(String accountName) throws IOException {
    Request request = new Request.Builder()
        .url(HttpUrl.parse(baseUri + "/accounts/" + accountName + "/balance"))
        .get()
        .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)))
        .build();
   try (Response response = httpClient.newCall(request).execute()) {
     assertThat(response.code()).isEqualTo(200);
     return objectMapper.readValue(response.body().string(), BalanceResponse.class).getBalance();
   }
  }

  @Immutable
  @JsonDeserialize(as = ImmutableBalanceResponse.class)
  @JsonSerialize(as = ImmutableBalanceResponse.class)
  public static interface BalanceResponse {
    BigDecimal getBalance();
  }

}
