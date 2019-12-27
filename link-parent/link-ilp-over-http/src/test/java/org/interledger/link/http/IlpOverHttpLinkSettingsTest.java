package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link IlpOverHttpLinkSettings}.
 */
public class IlpOverHttpLinkSettingsTest extends AbstractHttpLinkSettingsTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsSimpleWithFlatDottedNotation() {
    final Map<String, Object> flattenedCustomSettings = this.customSettingsSimpleFlat();

    final ImmutableIlpOverHttpLinkSettings.Builder builder = IlpOverHttpLinkSettings.builder();
    final IlpOverHttpLinkSettings ilpOverHttpLinkSettings =
        IlpOverHttpLinkSettings.applyCustomSettings(builder, flattenedCustomSettings).build();

    assertThat(ilpOverHttpLinkSettings.incomingLinkSettings().get().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    SimpleAuthSettings incomingSimpleSettings =
        ilpOverHttpLinkSettings.incomingLinkSettings().get().simpleAuthSettings().get();
    assertThat(incomingSimpleSettings.authToken()).isEqualTo("incoming-secret");

    SimpleAuthSettings outgoingSimpleSettings =
        ilpOverHttpLinkSettings.outgoingLinkSettings().get().simpleAuthSettings().get();
    assertThat(outgoingSimpleSettings.authToken()).isEqualTo("outgoing-secret");
    assertThat(ilpOverHttpLinkSettings.outgoingLinkSettings().get().url())
        .isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));

    assertThat(ilpOverHttpLinkSettings.toCustomSettingsMap()).isEqualTo(flattenedCustomSettings);
  }

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsSimpleWithMapHeirarchy() {
    final Map<String, Object> hierchicalSettings = this.customSettingsSimpleHierarchical();

    final ImmutableIlpOverHttpLinkSettings.Builder builder = IlpOverHttpLinkSettings.builder();
    final IlpOverHttpLinkSettings ilpOverHttpLinkSettings =
        IlpOverHttpLinkSettings.applyCustomSettings(builder, hierchicalSettings).build();

    assertThat(ilpOverHttpLinkSettings.incomingLinkSettings().get().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    SimpleAuthSettings incomingSimpleSettings =
        ilpOverHttpLinkSettings.incomingLinkSettings().get().simpleAuthSettings().get();
    assertThat(incomingSimpleSettings.authToken()).isEqualTo("incoming-secret");

    SimpleAuthSettings outgoingSimpleSettings =
        ilpOverHttpLinkSettings.outgoingLinkSettings().get().simpleAuthSettings().get();
    assertThat(outgoingSimpleSettings.authToken()).isEqualTo("outgoing-secret");
    assertThat(ilpOverHttpLinkSettings.outgoingLinkSettings().get().url())
        .isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));

    assertThat(ilpOverHttpLinkSettings.toCustomSettingsMap())
        .isEqualTo(LinkSettingsUtils.flattenSettings(hierchicalSettings));
  }

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsJwtWithFlatDottedNotation() {
    final Map<String, Object> flattenedCustomSettings =
        this.customSettingsJwtFlat(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);

    final ImmutableIlpOverHttpLinkSettings.Builder builder = IlpOverHttpLinkSettings.builder();
    final IlpOverHttpLinkSettings ilpOverHttpLinkSettings =
        IlpOverHttpLinkSettings.applyCustomSettings(builder, flattenedCustomSettings).build();

    assertThat(ilpOverHttpLinkSettings.incomingLinkSettings().get().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    JwtAuthSettings incomingJwtSettings = ilpOverHttpLinkSettings.incomingLinkSettings().get().jwtAuthSettings().get();
    assertThat(incomingJwtSettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(incomingJwtSettings.tokenAudience().get()).isEqualTo("https://incoming-audience.example.com/");

    JwtAuthSettings outgoingJwtSettings = ilpOverHttpLinkSettings.outgoingLinkSettings().get().jwtAuthSettings().get();
    assertThat(outgoingJwtSettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(incomingJwtSettings.encryptedTokenSharedSecret())
        .isEqualTo(Optional.of("incoming-credential"));
    assertThat(ilpOverHttpLinkSettings.incomingLinkSettings().get().getMinMessageWindow())
        .isEqualTo(Duration.ofMillis(2500));
    assertThat(ilpOverHttpLinkSettings.outgoingLinkSettings().get().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    assertThat(outgoingJwtSettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(outgoingJwtSettings.tokenAudience().get()).isEqualTo("https://outgoing-audience.example.com/");
    assertThat(outgoingJwtSettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(outgoingJwtSettings.encryptedTokenSharedSecret())
        .isEqualTo(Optional.of("outgoing-credential"));
    assertThat(outgoingJwtSettings.tokenExpiry().get())
      .isEqualTo(Duration.ofHours(24));
    assertThat(ilpOverHttpLinkSettings.outgoingLinkSettings().get().url())
        .isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));

    assertThat(ilpOverHttpLinkSettings.toCustomSettingsMap()).isEqualTo(flattenedCustomSettings);
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsJwtWithMapHeirarchy() {
    final Map<String, Object> customSettings =
        this.customSettingsJwtFlat(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);


    final ImmutableIlpOverHttpLinkSettings.Builder builder = IlpOverHttpLinkSettings.builder();
    final ImmutableIlpOverHttpLinkSettings httpLinkSettings =
        IlpOverHttpLinkSettings.applyCustomSettings(builder, customSettings).build();

    assertThat(httpLinkSettings.incomingLinkSettings().get().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    JwtAuthSettings incomingJwtAuthSettings = httpLinkSettings.incomingLinkSettings().get().jwtAuthSettings().get();
    assertThat(incomingJwtAuthSettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(incomingJwtAuthSettings.tokenAudience().get())
        .isEqualTo("https://incoming-audience.example.com/");
    assertThat(incomingJwtAuthSettings.encryptedTokenSharedSecret())
        .isEqualTo(Optional.of("incoming-credential"));
    assertThat(httpLinkSettings.incomingLinkSettings().get().getMinMessageWindow()).isEqualTo(Duration.ofMillis(2500));

    assertThat(httpLinkSettings.outgoingLinkSettings().get().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    JwtAuthSettings outgoingJwtAuthSettings = httpLinkSettings.outgoingLinkSettings().get().jwtAuthSettings().get();
    assertThat(outgoingJwtAuthSettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(outgoingJwtAuthSettings.tokenAudience().get())
        .isEqualTo("https://outgoing-audience.example.com/");
    assertThat(outgoingJwtAuthSettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(outgoingJwtAuthSettings.encryptedTokenSharedSecret())
        .isEqualTo(Optional.of("outgoing-credential"));
    assertThat(outgoingJwtAuthSettings.tokenExpiry().get()).isEqualTo(Duration.ofHours(24));
    assertThat(httpLinkSettings.outgoingLinkSettings().get().url())
        .isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));

    assertThat(httpLinkSettings.toCustomSettingsMap())
        .isEqualTo(LinkSettingsUtils.flattenSettings(customSettings));
  }

  @Test
  public void testWithoutCustomSettings() {
    final IncomingLinkSettings incomingLinksettings =
        IncomingLinkSettings.builder()
            .minMessageWindow(Duration.ofMillis(30))
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .simpleAuthSettings(
                SimpleAuthSettings.forAuthToken("incoming-credential")
            )
            .build();

    final OutgoingLinkSettings outgoingLinksettings =
        OutgoingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .simpleAuthSettings(
                SimpleAuthSettings.forAuthToken("outgoing-credential")
            )
            .url(HttpUrl.parse("https://outgoing.example.com/"))
            .build();

    final IlpOverHttpLinkSettings ilpOverHttpLinkSettings = IlpOverHttpLinkSettings.builder()
        .incomingLinkSettings(incomingLinksettings)
        .outgoingLinkSettings(outgoingLinksettings)
        .build();

    assertThat(ilpOverHttpLinkSettings.getLinkType()).isEqualTo(IlpOverHttpLink.LINK_TYPE);

    assertThat(ilpOverHttpLinkSettings.incomingLinkSettings().get().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(ilpOverHttpLinkSettings.incomingLinkSettings().get().simpleAuthSettings().get().authToken())
        .isEqualTo("incoming-credential");
    assertThat(ilpOverHttpLinkSettings.incomingLinkSettings().get().getMinMessageWindow())
        .isEqualTo(Duration.ofMillis(30));

    assertThat(ilpOverHttpLinkSettings.outgoingLinkSettings().get().authType())
        .isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(ilpOverHttpLinkSettings.outgoingLinkSettings().get().simpleAuthSettings().get().authToken())
        .isEqualTo("outgoing-credential");
    assertThat(ilpOverHttpLinkSettings.outgoingLinkSettings().get().url())
        .isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));
  }

}
