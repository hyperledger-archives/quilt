package org.interledger.link.http.auth;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;

/**
 * Unit tests for {@link JwtHs256BearerTokenSupplier}.
 */
public class JwtHs256BearerTokenSupplierTest {

  private static final byte[] EMPTY_BYTES = new byte[32];

  @Mock
  private SharedSecretBytesSupplier secretBytesSupplier;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(secretBytesSupplier.get()).thenReturn(EMPTY_BYTES);
  }

  @Test
  public void checkCaching() {
    OutgoingLinkSettings linkSettings = createOutgoingSettings(Duration.ofMinutes(5));
    JwtHs256BearerTokenSupplier tokenSupplier = new JwtHs256BearerTokenSupplier(secretBytesSupplier, linkSettings);
    tokenSupplier.get();
    tokenSupplier.get();
    verify(secretBytesSupplier).get();
  }

  @Test
  public void checkCachingWithCachingDisabled() {
    OutgoingLinkSettings linkSettings = createOutgoingSettings(Duration.ofMinutes(0));
    JwtHs256BearerTokenSupplier tokenSupplier = new JwtHs256BearerTokenSupplier(secretBytesSupplier, linkSettings);
    tokenSupplier.get();
    tokenSupplier.get();
    verify(secretBytesSupplier, times(2)).get();
  }

  @Test
  public void checkCacheExpiry() throws Exception {
    OutgoingLinkSettings linkSettings = createOutgoingSettings(Duration.ofMillis(2));
    JwtHs256BearerTokenSupplier tokenSupplier = new JwtHs256BearerTokenSupplier(secretBytesSupplier, linkSettings);
    tokenSupplier.get();
    Thread.sleep(3);
    tokenSupplier.get();
    verify(secretBytesSupplier, times(2)).get();
  }

  private OutgoingLinkSettings createOutgoingSettings(Duration duration) {
    return OutgoingLinkSettings.builder()
        .authType(IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
        .encryptedTokenSharedSecret("i am a terrible bowler")
        .tokenAudience(HttpUrl.get("https://www.ripple.com"))
        .tokenExpiry(duration)
        .tokenIssuer(HttpUrl.get("https://www.ripple.com"))
        .tokenSubject("about last night")
        .url(HttpUrl.get("https://www.ripple.com"))
        .build();
  }
}
