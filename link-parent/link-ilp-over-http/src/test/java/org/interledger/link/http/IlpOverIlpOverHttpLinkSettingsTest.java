package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

/**
 * Unit tests for {@link IlpOverHttpLinkSettings}.
 */
public class IlpOverIlpOverHttpLinkSettingsTest extends AbstractHttpLinkSettingsTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    final Map<String, Object> flattenedCustomSettings = this.customSettingsFlat();

    final ImmutableIlpOverHttpLinkSettings.Builder builder = IlpOverHttpLinkSettings.builder();
    final IlpOverHttpLinkSettings ilpOverHttpLinkSettings =
        IlpOverHttpLinkSettings.applyCustomSettings(builder, flattenedCustomSettings).build();

    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://incoming-audience.example.com/"));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret())
        .isEqualTo("incoming-credential");
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow())
        .isEqualTo(Duration.ofMillis(2500));

    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com/"));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret())
        .isEqualTo("outgoing-credential");
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenExpiry().get()).isEqualTo(Duration.ofHours(24));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().url())
        .isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> customSettings = this.customSettingsHeirarchical();

    final ImmutableIlpOverHttpLinkSettings.Builder builder = IlpOverHttpLinkSettings.builder();
    final ImmutableIlpOverHttpLinkSettings httpLinkSettings =
        IlpOverHttpLinkSettings.applyCustomSettings(builder, customSettings).build();

    assertThat(httpLinkSettings.incomingHttpLinkSettings().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://incoming-audience.example.com/"));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret())
        .isEqualTo("incoming-credential");
    assertThat(httpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow()).isEqualTo(Duration.ofMillis(2500));

    assertThat(httpLinkSettings.outgoingHttpLinkSettings().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com/"));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret())
        .isEqualTo("outgoing-credential");
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenExpiry().get()).isEqualTo(Duration.ofHours(48));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().url())
        .isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));
  }

  @Test
  public void testWithoutCustomSettings() {
    final IncomingLinkSettings incomingLinksettings =
        IncomingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .tokenIssuer(HttpUrl.parse("https://incoming-issuer.example.com/"))
            .tokenAudience(HttpUrl.parse("https://incoming-audience.example.com/"))
            .minMessageWindow(Duration.ofMillis(30))
            .encryptedTokenSharedSecret("incoming-credential")
            .build();

    final OutgoingLinkSettings outgoingLinksettings =
        OutgoingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .tokenSubject("outgoing-subject")
            .tokenIssuer(HttpUrl.parse("https://outgoing-issuer.example.com/"))
            .tokenAudience(HttpUrl.parse("https://outgoing-audience.example.com/"))
            .encryptedTokenSharedSecret("outgoing-credential")
            .tokenExpiry(Duration.ofMillis(40))
            .url(HttpUrl.parse("https://outgoing.example.com/"))
            .build();

    final IlpOverHttpLinkSettings ilpOverHttpLinkSettings = IlpOverHttpLinkSettings.builder()
        .incomingHttpLinkSettings(incomingLinksettings)
        .outgoingHttpLinkSettings(outgoingLinksettings)
        .build();

    assertThat(ilpOverHttpLinkSettings.getLinkType()).isEqualTo(IlpOverHttpLink.LINK_TYPE);

    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://incoming-audience.example.com/"));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret())
        .isEqualTo("incoming-credential");
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow())
        .isEqualTo(Duration.ofMillis(30));

    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com/"));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret())
        .isEqualTo("outgoing-credential");
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenExpiry().get()).isEqualTo(Duration.ofMillis(40));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().url())
        .isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));
  }
}
