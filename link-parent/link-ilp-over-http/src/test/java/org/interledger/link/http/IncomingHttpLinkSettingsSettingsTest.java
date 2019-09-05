package org.interledger.link.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

/**
 * Unit tests for {@link IncomingLinkSettings}.
 */
public class IncomingHttpLinkSettingsSettingsTest extends AbstractHttpLinkSettingsTest {

  // This value doesn't _strictly_ need to be encrypted for purposes of this test. It could easily be plain-text, but
  // for completeness we use the encrypted test-variant.
  private static final String SHH
      = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    final Map<String, Object> customSettings = this.customSettingsFlat();
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType(), is(HttpLinkSettings.AuthType.JWT_HS_256));
    assertThat(incomingLinksettings.tokenIssuer().get(), is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(incomingLinksettings.tokenAudience().get(), is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret(), is("incoming-credential"));
    assertThat(incomingLinksettings.getMinMessageWindow(), is(Duration.ofSeconds(1)));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> customSettings = this.customSettingsHeirarchical();
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType(), is(HttpLinkSettings.AuthType.JWT_HS_256));
    assertThat(incomingLinksettings.tokenIssuer().get(), is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(incomingLinksettings.tokenAudience().get(), is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret(), is("incoming-credential"));
    assertThat(incomingLinksettings.getMinMessageWindow(), is(Duration.ofSeconds(1)));
  }

  @Test
  public void testWithoutCustomSettings() {
    final IncomingLinkSettings incomingLinksettings =
        IncomingLinkSettings.builder()
            .authType(HttpLinkSettings.AuthType.SIMPLE)
            .tokenIssuer(HttpUrl.parse("https://incoming-issuer.example.com"))
            .tokenAudience(HttpUrl.parse("https://incoming-audience.example.com/"))
            .encryptedTokenSharedSecret(SHH)
            .minMessageWindow(Duration.ofMillis(30))
            .build();

    assertThat(incomingLinksettings.authType(), is(HttpLinkSettings.AuthType.SIMPLE));
    assertThat(incomingLinksettings.tokenIssuer().get(), is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(incomingLinksettings.tokenAudience().get(), is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret(), is(SHH));
    assertThat(incomingLinksettings.getMinMessageWindow(), is(Duration.ofMillis(30)));
  }
}
