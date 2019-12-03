package org.interledger.spsp.client.rust;

import org.interledger.spsp.client.SpspClientDefaults;
import org.interledger.spsp.client.InvalidReceiverClientException;
import org.interledger.spsp.client.SpspClientException;

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

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Client for interacting with API on a Rust Interledger Node.
 */
public class InterledgerRustNodeClient {

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient httpClient;
  private final String authToken;
  private final ObjectMapper objectMapper;
  private final String baseUri;

  public InterledgerRustNodeClient(OkHttpClient okHttpClient,
                                   String authToken,
                                   String baseUri) {
    this.httpClient = okHttpClient;
    this.authToken = authToken;
    this.objectMapper = SpspClientDefaults.MAPPER;
    this.baseUri = baseUri;
  }

  public RustNodeAccount createAccount(RustNodeAccount rustNodeAccount) throws IOException {
    return execute(requestBuilder()
      .url(HttpUrl.parse(baseUri + "/accounts"))
      .post(RequestBody.create(objectMapper.writeValueAsString(rustNodeAccount), JSON))
      .build(), RustNodeAccount.class);
  }

  private Request.Builder requestBuilder() {
    return new Request.Builder()
      .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)));
  }

  public BigDecimal getBalance(String accountName) throws SpspClientException {
    return execute(requestBuilder()
      .url(HttpUrl.parse(baseUri + "/accounts/" + accountName + "/balance"))
      .get()
      .build(), BalanceResponse.class)
      .getBalance();
  }

  private <T> T execute(Request request, Class<T> clazz) throws SpspClientException {
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        if (response.code() == 404) {
          throw new InvalidReceiverClientException(request.url().toString());
        }
        throw new SpspClientException("Received non-successful HTTP response code " + response.code()
          + " calling " + request.url());
      }
      return objectMapper.readValue(response.body().string(), clazz);
    } catch (IOException e) {
      throw new SpspClientException("IOException failure calling " + request.url(), e);
    }
  }

  @Immutable
  @JsonDeserialize(builder = ImmutableBalanceResponse.Builder.class)
  @JsonSerialize(as = ImmutableBalanceResponse.class)
  public interface BalanceResponse {
    BigDecimal getBalance();
  }

}
