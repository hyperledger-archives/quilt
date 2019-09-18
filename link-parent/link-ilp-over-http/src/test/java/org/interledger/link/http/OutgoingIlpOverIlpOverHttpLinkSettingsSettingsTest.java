package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

/**
 * Unit tests for {@link OutgoingLinkSettings}.
 */
public class OutgoingIlpOverIlpOverHttpLinkSettingsSettingsTest extends AbstractHttpLinkSettingsTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    final Map<String, Object> customSettings = this.customSettingsFlat();
    final OutgoingLinkSettings outgoingLinksettings = OutgoingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(outgoingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(outgoingLinksettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(outgoingLinksettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com/"));
    assertThat(outgoingLinksettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(outgoingLinksettings.encryptedTokenSharedSecret()).isEqualTo("outgoing-credential");
    assertThat(outgoingLinksettings.tokenExpiry().get()).isEqualTo(Duration.ofHours(24));
    assertThat(outgoingLinksettings.url()).isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> customSettings = this.customSettingsHeirarchical();
    final OutgoingLinkSettings outgoingLinksettings = OutgoingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(outgoingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(outgoingLinksettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(outgoingLinksettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com/"));
    assertThat(outgoingLinksettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(outgoingLinksettings.encryptedTokenSharedSecret()).isEqualTo("outgoing-credential");
    assertThat(outgoingLinksettings.tokenExpiry().get()).isEqualTo(Duration.ofHours(48));
    assertThat(outgoingLinksettings.url()).isEqualTo(HttpUrl.parse("https://outgoing.example.com"));
  }

  @Test
  public void testWithoutCustomSettings() {
    final OutgoingLinkSettings outgoingLinksettings =
        OutgoingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .tokenIssuer(HttpUrl.parse("https://outgoing-issuer.example.com"))
            .tokenAudience(HttpUrl.parse("https://outgoing-audience.example.com"))
            .tokenSubject("outgoing-subject")
            .tokenExpiry(Duration.ofMillis(30))
            .encryptedTokenSharedSecret("outgoing-credential")
            .url(HttpUrl.parse("https://outgoing.example.com"))
            .build();

    assertThat(outgoingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(outgoingLinksettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com"));
    assertThat(outgoingLinksettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com"));
    assertThat(outgoingLinksettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(outgoingLinksettings.tokenExpiry().get()).isEqualTo(Duration.ofMillis(30));
    assertThat(outgoingLinksettings.encryptedTokenSharedSecret()).isEqualTo("outgoing-credential");
    assertThat(outgoingLinksettings.url()).isEqualTo(HttpUrl.parse("https://outgoing.example.com"));
  }
}
