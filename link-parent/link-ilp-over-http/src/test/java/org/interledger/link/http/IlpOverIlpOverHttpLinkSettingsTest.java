package org.interledger.link.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().authType(),
        is(IlpOverHttpLinkSettings.AuthType.JWT_HS_256));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret(),
        is("incoming-credential"));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow(), is(Duration.ofMillis(2500)));

    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().authType(),
        is(IlpOverHttpLinkSettings.AuthType.SIMPLE));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenSubject(), is("outgoing-subject"));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret(),
        is("outgoing-credential"));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenExpiry().get(), is(Duration.ofHours(24)));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().url(),
        is(HttpUrl.parse("https://outgoing.example.com/")));
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

    assertThat(httpLinkSettings.incomingHttpLinkSettings().authType(), is(IlpOverHttpLinkSettings.AuthType.JWT_HS_256));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret(),
        is("incoming-credential"));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow(), is(Duration.ofMillis(2500)));

    assertThat(httpLinkSettings.outgoingHttpLinkSettings().authType(), is(IlpOverHttpLinkSettings.AuthType.SIMPLE));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenSubject(), is("outgoing-subject"));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret(),
        is("outgoing-credential"));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenExpiry().get(), is(Duration.ofHours(48)));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().url(), is(HttpUrl.parse("https://outgoing.example.com/")));
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

    assertThat(ilpOverHttpLinkSettings.getLinkType(), is(IlpOverHttpLink.LINK_TYPE));

    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().authType(),
        is(IlpOverHttpLinkSettings.AuthType.SIMPLE));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret(),
        is("incoming-credential"));
    assertThat(ilpOverHttpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow(), is(Duration.ofMillis(30)));

    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().authType(),
        is(IlpOverHttpLinkSettings.AuthType.SIMPLE));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenSubject(), is("outgoing-subject"));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret(),
        is("outgoing-credential"));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().tokenExpiry().get(), is(Duration.ofMillis(40)));
    assertThat(ilpOverHttpLinkSettings.outgoingHttpLinkSettings().url(),
        is(HttpUrl.parse("https://outgoing.example.com/")));
  }
}
