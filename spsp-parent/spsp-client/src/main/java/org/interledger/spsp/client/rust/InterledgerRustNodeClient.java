package org.interledger.spsp.client.rust;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.immutables.value.Value.Immutable;
import org.interledger.spsp.client.InvalidReceiverClientException;
import org.interledger.spsp.client.SpspClientDefaults;
import org.interledger.spsp.client.SpspClientException;

/**
 * Client for interacting with API on a Rust Interledger Node.
 */
public class InterledgerRustNodeClient {

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient httpClient;
  private final String authToken;
  private final ObjectMapper objectMapper;
  private final HttpUrl baseUrl;

  /**
   * Required-args constructor.
   *
   * @param okHttpClient A {@link OkHttpClient}.
   * @param authToken    An authentication token for the Rust Connector.
   * @param baseUrl      An {@link String} that contains the Connector's base URL.
   * @deprecated Use the constructor that accepts a {@link HttpUrl} instead.
   */
  @Deprecated
  public InterledgerRustNodeClient(OkHttpClient okHttpClient, String authToken, String baseUrl) {
    this(okHttpClient, authToken, HttpUrl.parse(baseUrl));
  }

  /**
   * Required-args constructor.
   *
   * @param okHttpClient A {@link OkHttpClient}.
   * @param authToken    An authentication token for the Rust Connector.
   * @param baseUrl      An {@link HttpUrl} that contains the Connector's base URL.
   */
  public InterledgerRustNodeClient(
    final OkHttpClient okHttpClient, final String authToken, final HttpUrl baseUrl
  ) {
    this.httpClient = Objects.requireNonNull(okHttpClient);
    this.authToken = Objects.requireNonNull(authToken);
    this.objectMapper = Objects.requireNonNull(SpspClientDefaults.MAPPER);
    this.baseUrl = Objects.requireNonNull(baseUrl);
  }

  public RustNodeAccount createAccount(RustNodeAccount rustNodeAccount) throws IOException {
    return execute(requestBuilder()
      .url(baseUrl.newBuilder().addPathSegment("accounts").build())
      .post(RequestBody.create(objectMapper.writeValueAsString(rustNodeAccount), JSON))
      .build(), RustNodeAccount.class);
  }

  public UsdToXrpRatesResponse setUsdToXrpRate(UsdToXrpRatesRequest usDtoXRPRatesRequest) throws IOException {
    return execute(requestBuilder()
      .url(baseUrl.newBuilder().addPathSegment("rates").build())
      .put(RequestBody.create(objectMapper.writeValueAsString(usDtoXRPRatesRequest), JSON))
      .build(), UsdToXrpRatesResponse.class);
  }

  private Request.Builder requestBuilder() {
    return new Request.Builder()
      .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)));
  }

  public BigDecimal getBalance(String accountName) throws SpspClientException {
    return execute(requestBuilder()
      .url(
        baseUrl.newBuilder().addPathSegment("accounts").addPathSegment(accountName).addPathSegment("balance").build()
      )
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
