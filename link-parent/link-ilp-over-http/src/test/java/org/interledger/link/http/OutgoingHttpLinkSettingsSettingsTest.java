package org.interledger.link.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

/**
 * Unit tests for {@link OutgoingLinkSettings}.
 */
public class OutgoingHttpLinkSettingsSettingsTest extends AbstractHttpLinkSettingsTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    final Map<String, Object> customSettings = this.customSettingsFlat();
    final OutgoingLinkSettings outgoingLinksettings = OutgoingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(outgoingLinksettings.authType(), is(HttpLinkSettings.AuthType.SIMPLE));
    assertThat(outgoingLinksettings.tokenIssuer().get(), is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(outgoingLinksettings.tokenAudience().get(), is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(outgoingLinksettings.tokenSubject(), is("outgoing-subject"));
    assertThat(outgoingLinksettings.encryptedTokenSharedSecret(), is("outgoing-credential"));
    assertThat(outgoingLinksettings.tokenExpiry().get(), is(Duration.ofHours(24)));
    assertThat(outgoingLinksettings.url(), is(HttpUrl.parse("https://outgoing.example.com/")));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> customSettings = this.customSettingsHeirarchical();
    final OutgoingLinkSettings outgoingLinksettings = OutgoingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(outgoingLinksettings.authType(), is(HttpLinkSettings.AuthType.SIMPLE));
    assertThat(outgoingLinksettings.tokenIssuer().get(), is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(outgoingLinksettings.tokenAudience().get(), is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(outgoingLinksettings.tokenSubject(), is("outgoing-subject"));
    assertThat(outgoingLinksettings.encryptedTokenSharedSecret(), is("outgoing-credential"));
    assertThat(outgoingLinksettings.tokenExpiry().get(), is(Duration.ofHours(48)));
    assertThat(outgoingLinksettings.url(), is(HttpUrl.parse("https://outgoing.example.com")));
  }

  @Test
  public void testWithoutCustomSettings() {
    final OutgoingLinkSettings outgoingLinksettings =
      OutgoingLinkSettings.builder()
        .authType(HttpLinkSettings.AuthType.SIMPLE)
        .tokenIssuer(HttpUrl.parse("https://outgoing-issuer.example.com"))
        .tokenAudience(HttpUrl.parse("https://outgoing-audience.example.com"))
        .tokenSubject("outgoing-subject")
        .tokenExpiry(Duration.ofMillis(30))
        .encryptedTokenSharedSecret("outgoing-credential")
        .url(HttpUrl.parse("https://outgoing.example.com"))
        .build();

    assertThat(outgoingLinksettings.authType(), is(HttpLinkSettings.AuthType.SIMPLE));
    assertThat(outgoingLinksettings.tokenIssuer().get(), is(HttpUrl.parse("https://outgoing-issuer.example.com")));
    assertThat(outgoingLinksettings.tokenAudience().get(), is(HttpUrl.parse("https://outgoing-audience.example.com")));
    assertThat(outgoingLinksettings.tokenSubject(), is("outgoing-subject"));
    assertThat(outgoingLinksettings.tokenExpiry().get(), is(Duration.ofMillis(30)));
    assertThat(outgoingLinksettings.encryptedTokenSharedSecret(), is("outgoing-credential"));
    assertThat(outgoingLinksettings.url(), is(HttpUrl.parse("https://outgoing.example.com")));
  }
}
