package org.interledger.spsp.client;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.spsp.StreamConnectionDetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

/**
 * Basic implementation of an {@link SpspClient} that uses default okhttp3 client and jackson mapper.
 */
public class SimpleSpspClient implements SpspClient {

  private final OkHttpClient httpClient;

  private final PaymentPointerResolver paymentPointerResolver;

  private final ObjectMapper objectMapper;

  public SimpleSpspClient() {
    this(SpspClientDefaults.OK_HTTP_CLIENT, PaymentPointerResolver.defaultResolver(), SpspClientDefaults.MAPPER);
  }

  public SimpleSpspClient(
    final OkHttpClient httpClient, final PaymentPointerResolver paymentPointerResolver, final ObjectMapper objectMapper
  ) {
    this.httpClient = Objects.requireNonNull(httpClient);
    this.paymentPointerResolver = Objects.requireNonNull(paymentPointerResolver);
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  @Override
  public StreamConnectionDetails getStreamConnectionDetails(final PaymentPointer paymentPointer)
    throws InvalidReceiverClientException {
    Objects.requireNonNull(paymentPointer);
    return getStreamConnectionDetails(paymentPointerResolver.resolveHttpUrl(paymentPointer));
  }

  @Override
  public StreamConnectionDetails getStreamConnectionDetails(final HttpUrl spspUrl)
    throws InvalidReceiverClientException {
    Objects.requireNonNull(spspUrl);

    return execute(new Request.Builder()
      .url(spspUrl)
      .header("Accept", APPLICATION_SPSP4_JSON_VALUE)
      .get()
      .build(), StreamConnectionDetails.class);
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

}
