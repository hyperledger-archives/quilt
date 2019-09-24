package org.interledger.spsp.client.rust;

import org.interledger.quilt.jackson.InterledgerModule;
import org.interledger.quilt.jackson.conditions.Encoding;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.InvalidReceiverClientException;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.client.SpspClientException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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

public class InterledgerRustNodeClient implements SpspClient {

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient httpClient;
  private final String authToken;
  private final ObjectMapper objectMapper;
  private final String baseUri;
  private final PaymentPointerResolver paymentPointerResolver;

  private static final ObjectMapper MAPPER = mapper();

  private static ObjectMapper mapper() {
    final ObjectMapper objectMapper = JsonMapper.builder()
        .serializationInclusion(JsonInclude.Include.NON_EMPTY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS, false)
        .build()
        .registerModule(new Jdk8Module())
        .registerModule(new InterledgerModule(Encoding.BASE64));
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    return objectMapper;
  }

  public InterledgerRustNodeClient(OkHttpClient okHttpClient,
                                   String authToken,
                                   String baseUri,
                                   PaymentPointerResolver paymentPointerResolver) {
    this.httpClient = okHttpClient;
    this.authToken = authToken;
    this.objectMapper = MAPPER;
    this.baseUri = baseUri;
    this.paymentPointerResolver = paymentPointerResolver;
  }

  public Account createAccount(Account account) throws IOException {
    return execute(requestBuilder()
      .url(HttpUrl.parse(baseUri + "/accounts"))
      .post(RequestBody.create(objectMapper.writeValueAsString(account), JSON))
      .build(), Account.class);
  }

  private Request.Builder requestBuilder() {
    return new Request.Builder()
      .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)));
  }

  public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
      throws InvalidReceiverClientException {
    return execute(requestBuilder()
      .url(HttpUrl.parse(paymentPointerResolver.resolve(paymentPointer)))
      .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken,
        "Accept", ACCEPT_SPSP_JSON)))
      .get()
      .build(), StreamConnectionDetails.class);
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
