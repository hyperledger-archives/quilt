package org.interledger.link.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

/**
 * Unit tests for {@link HttpLinkSettings}.
 */
public class HttpLinkSettingsTest extends AbstractHttpLinkSettingsTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    final Map<String, Object> flattenedCustomSettings = this.customSettingsFlat();

    final ImmutableHttpLinkSettings.Builder builder = HttpLinkSettings.builder().linkType(HttpLink.LINK_TYPE);
    final HttpLinkSettings httpLinkSettings =
        HttpLinkSettings.applyCustomSettings(builder, flattenedCustomSettings).build();

    assertThat(httpLinkSettings.incomingHttpLinkSettings().authType(), is(HttpLinkSettings.AuthType.JWT_HS_256));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret(), is("incoming-credential"));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow(), is(Duration.ofSeconds(1)));

    assertThat(httpLinkSettings.outgoingHttpLinkSettings().authType(), is(HttpLinkSettings.AuthType.SIMPLE));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenSubject(), is("outgoing-subject"));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret(), is("outgoing-credential"));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenExpiry().get(), is(Duration.ofHours(24)));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().url(), is(HttpUrl.parse("https://outgoing.example.com/")));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> customSettings = this.customSettingsHeirarchical();

    final ImmutableHttpLinkSettings.Builder builder = HttpLinkSettings.builder().linkType(HttpLink.LINK_TYPE);
    final ImmutableHttpLinkSettings httpLinkSettings =
        HttpLinkSettings.applyCustomSettings(builder, customSettings).build();

    assertThat(httpLinkSettings.incomingHttpLinkSettings().authType(), is(HttpLinkSettings.AuthType.JWT_HS_256));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret(),
        is("incoming-credential"));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow(), is(Duration.ofSeconds(1)));

    assertThat(httpLinkSettings.outgoingHttpLinkSettings().authType(), is(HttpLinkSettings.AuthType.SIMPLE));
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
            .authType(HttpLinkSettings.AuthType.SIMPLE)
            .tokenIssuer(HttpUrl.parse("https://incoming-issuer.example.com/"))
            .tokenAudience(HttpUrl.parse("https://incoming-audience.example.com/"))
            .minMessageWindow(Duration.ofMillis(30))
            .encryptedTokenSharedSecret("incoming-credential")
            .build();

    final OutgoingLinkSettings outgoingLinksettings =
        OutgoingLinkSettings.builder()
            .authType(HttpLinkSettings.AuthType.SIMPLE)
            .tokenSubject("outgoing-subject")
            .tokenIssuer(HttpUrl.parse("https://outgoing-issuer.example.com/"))
            .tokenAudience(HttpUrl.parse("https://outgoing-audience.example.com/"))
            .encryptedTokenSharedSecret("outgoing-credential")
            .tokenExpiry(Duration.ofMillis(40))
            .url(HttpUrl.parse("https://outgoing.example.com/"))
            .build();

    final HttpLinkSettings httpLinkSettings = HttpLinkSettings.builder()
        .linkType(HttpLink.LINK_TYPE)
        .incomingHttpLinkSettings(incomingLinksettings)
        .outgoingHttpLinkSettings(outgoingLinksettings)
        .build();

    assertThat(httpLinkSettings.getLinkType(), is(HttpLink.LINK_TYPE));

    assertThat(httpLinkSettings.incomingHttpLinkSettings().authType(), is(HttpLinkSettings.AuthType.SIMPLE));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret(), is("incoming-credential"));
    assertThat(httpLinkSettings.incomingHttpLinkSettings().getMinMessageWindow(), is(Duration.ofMillis(30)));

    assertThat(httpLinkSettings.outgoingHttpLinkSettings().authType(), is(HttpLinkSettings.AuthType.SIMPLE));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenIssuer().get(),
        is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenAudience().get(),
        is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenSubject(), is("outgoing-subject"));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret(), is("outgoing-credential"));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().tokenExpiry().get(), is(Duration.ofMillis(40)));
    assertThat(httpLinkSettings.outgoingHttpLinkSettings().url(),
        is(HttpUrl.parse("https://outgoing.example.com/")));
  }
}
