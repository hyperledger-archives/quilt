package org.interledger.link.http.auth;

import okhttp3.HttpUrl;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.junit.Test;

import java.time.Duration;

import static org.mockito.Mockito.*;

public class JwtHs256BearerTokenSupplierTest {

  @Test
  public void checkCaching() {
    SharedSecretBytesSupplier secretBytesSupplier = mock(SharedSecretBytesSupplier.class);
    byte[] muhBytes = new byte[32];
    when(secretBytesSupplier.get()).thenReturn(muhBytes);
    OutgoingLinkSettings linkSettings = createOutgoingSettings(Duration.ofSeconds(5));
    JwtHs256BearerTokenSupplier tokenSupplier = new JwtHs256BearerTokenSupplier(secretBytesSupplier, linkSettings);
    tokenSupplier.get();
    tokenSupplier.get();
    verify(secretBytesSupplier, times(1)).get();
  }

  @Test
  public void checkCacheExpiry() throws Exception {
    SharedSecretBytesSupplier secretBytesSupplier = mock(SharedSecretBytesSupplier.class);
    byte[] muhBytes = new byte[32];
    when(secretBytesSupplier.get()).thenReturn(muhBytes);
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
