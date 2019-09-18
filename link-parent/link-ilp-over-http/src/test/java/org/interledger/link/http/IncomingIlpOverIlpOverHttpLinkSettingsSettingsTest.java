package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

/**
 * Unit tests for {@link IncomingLinkSettings}.
 */
public class IncomingIlpOverIlpOverHttpLinkSettingsSettingsTest extends AbstractHttpLinkSettingsTest {

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

    assertThat(incomingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    assertThat(incomingLinksettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(incomingLinksettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://incoming-audience.example.com/"));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret()).isEqualTo("incoming-credential");
    assertThat(incomingLinksettings.getMinMessageWindow()).isEqualTo(Duration.ofMillis(2500));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> customSettings = this.customSettingsHeirarchical();
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    assertThat(incomingLinksettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(incomingLinksettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://incoming-audience.example.com/"));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret()).isEqualTo("incoming-credential");
    assertThat(incomingLinksettings.getMinMessageWindow()).isEqualTo(Duration.ofMillis(2500));
  }

  @Test
  public void testWithoutCustomSettings() {
    final IncomingLinkSettings incomingLinksettings =
        IncomingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .tokenIssuer(HttpUrl.parse("https://incoming-issuer.example.com"))
            .tokenAudience(HttpUrl.parse("https://incoming-audience.example.com/"))
            .encryptedTokenSharedSecret(SHH)
            .minMessageWindow(Duration.ofMillis(30))
            .build();

    assertThat(incomingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(incomingLinksettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(incomingLinksettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://incoming-audience.example.com/"));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret()).isEqualTo(SHH);
    assertThat(incomingLinksettings.getMinMessageWindow()).isEqualTo(Duration.ofMillis(30));
  }
}
