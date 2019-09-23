package org.interledger.spsp.client;

import okhttp3.*;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.spsp.StreamConnectionDetails;
import java.io.IOException;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value.Immutable;

public class InterledgerRustNodeClient implements SpspClient {

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient httpClient;
  private final String authToken;
  private final ObjectMapper objectMapper;
  private final String baseUri;
  private final PaymentPointerResolver paymentPointerResolver;

  public InterledgerRustNodeClient(OkHttpClient okHttpClient,
                                   String authToken,
                                   ObjectMapper objectMapper,
                                   String baseUri,
                                   PaymentPointerResolver paymentPointerResolver) {
    this.httpClient = okHttpClient;
    this.authToken = authToken;
    this.objectMapper = objectMapper;
    this.baseUri = baseUri;
    this.paymentPointerResolver = paymentPointerResolver;
  }

  public void createAccount(InterledgerAddress hostIlpAddress, String accountName) throws IOException {
    ImmutableMap<Object, Object> newAccountPayload = ImmutableMap.builder()
        .put("ilp_address", hostIlpAddress.with(accountName).getValue())
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

    execute(new Request.Builder()
        .url(HttpUrl.parse(baseUri + "/accounts"))
        .post(RequestBody.create(new ObjectMapper().writeValueAsString(newAccountPayload), JSON))
        .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)))
        .build(), Void.class);
  }

  public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer) throws InvalidReceiverException {
    return execute(new Request.Builder()
        .url(HttpUrl.parse(paymentPointerResolver.resolve(paymentPointer)))
        .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)))
        .get()
        .build(), StreamConnectionDetails.class);
  }


  public BigDecimal getBalance(String accountName) throws SpspException {
    return execute(new Request.Builder()
        .url(HttpUrl.parse(baseUri + "/accounts/" + accountName + "/balance"))
        .get()
        .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)))
        .build(), BalanceResponse.class)
      .getBalance();
  }

  private <T> T execute(Request request, Class<T> clazz) throws SpspException {
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        if (response.code() == 404) {
          throw new InvalidReceiverException(request.url().toString());
        }
        throw new SpspException("Received non-successful HTTP response code " +  response.code()
          + " calling " + request.url());
      }
      return objectMapper.readValue(response.body().string(), clazz);
    }
    catch (IOException e) {
      throw new SpspException("IOException failure calling " + request.url(), e);
    }
  }

  @Immutable
  @JsonDeserialize(as = ImmutableBalanceResponse.class)
  @JsonSerialize(as = ImmutableBalanceResponse.class)
  public interface BalanceResponse {
    BigDecimal getBalance();
  }

}
