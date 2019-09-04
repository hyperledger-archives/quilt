package org.interledger.link.http;

import static okhttp3.CookieJar.NO_COOKIES;

import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * <p>Configuration for{@link HttpSender}.</p>
 */
public interface IlpOverHttpClientConfig {

  int defaultMaxIdleConnections();

  long defaultConnectionKeepAliveMinutes();

  long defaultConnectTimeoutMillis();

  long defaultReadTimeoutMillis();

  long defaultWriteTimeoutMillis();

  // TODO: Move to factory class.
  default ConnectionPool constructConnectionPool() {
    return new ConnectionPool(defaultMaxIdleConnections(), defaultConnectionKeepAliveMinutes(), TimeUnit.MINUTES);
  }

  // TODO: Move to factory class.
  default OkHttpClient constructOkHttpClient() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);

    builder.connectTimeout(defaultConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
    builder.readTimeout(defaultReadTimeoutMillis(), TimeUnit.MILLISECONDS);
    builder.writeTimeout(defaultWriteTimeoutMillis(), TimeUnit.MILLISECONDS);

    return builder.connectionPool(constructConnectionPool()).build();
  }

  // TODO: Move to factory class.
  default HttpSender constructBlastHttpSender() {
    return null;
    //return new JwtBlastHttpSender();
  }
}
