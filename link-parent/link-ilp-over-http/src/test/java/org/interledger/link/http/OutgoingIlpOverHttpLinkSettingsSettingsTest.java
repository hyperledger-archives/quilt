package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link OutgoingLinkSettings}.
 */
public class OutgoingIlpOverHttpLinkSettingsSettingsTest extends AbstractHttpLinkSettingsTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    IlpOverHttpLinkSettings.AuthType authType = IlpOverHttpLinkSettings.AuthType.JWT_HS_256;

    final Map<String, Object> customSettings = this.customSettingsJwtFlat(authType);
    final OutgoingLinkSettings outgoingLinksettings = OutgoingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(outgoingLinksettings.authType()).isEqualTo(authType);
    JwtAuthSettings jwtAuthSettings = outgoingLinksettings.jwtAuthSettings().get();
    assertThat(jwtAuthSettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(jwtAuthSettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com/"));
    assertThat(jwtAuthSettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(jwtAuthSettings.encryptedTokenSharedSecret()).isEqualTo(Optional.of("outgoing-credential"));
    assertThat(jwtAuthSettings.tokenExpiry().get()).isEqualTo(Duration.ofHours(24));
    assertThat(outgoingLinksettings.url()).isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));
  }

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation,
   * ignoring properties not applicable to SIMPLE auth
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotationWithSimpleAuth() {
    final Map<String, Object> customSettings = this.customSettingsSimpleFlat();
    final OutgoingLinkSettings outgoingLinksettings = OutgoingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(outgoingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(outgoingLinksettings.simpleAuthSettings().get().authToken()).isEqualTo("outgoing-secret");
    assertThat(outgoingLinksettings.url()).isEqualTo(HttpUrl.parse("https://outgoing.example.com/"));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHierarchy() {
    IlpOverHttpLinkSettings.AuthType authType = IlpOverHttpLinkSettings.AuthType.JWT_HS_256;
    final Map<String, Object> customSettings = this.customSettingsJwtHierarchical(authType);
    final OutgoingLinkSettings outgoingLinksettings = OutgoingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(outgoingLinksettings.authType()).isEqualTo(authType);
    JwtAuthSettings jwtAuthSettings = outgoingLinksettings.jwtAuthSettings().get();
    assertThat(jwtAuthSettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com/"));
    assertThat(jwtAuthSettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com/"));
    assertThat(jwtAuthSettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(jwtAuthSettings.encryptedTokenSharedSecret()).isEqualTo(Optional.of("outgoing-credential"));
    assertThat(jwtAuthSettings.tokenExpiry().get()).isEqualTo(Duration.ofHours(48));
    assertThat(outgoingLinksettings.url()).isEqualTo(HttpUrl.parse("https://outgoing.example.com"));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps, ignoring properties not applicable to simple auth
   */
  @Test
  public void applyCustomSettingsWithMapHierarchyWithSimpleAuth() {
    final Map<String, Object> customSettings = this.customSettingsSimpleHierarchical();
    final OutgoingLinkSettings outgoingLinksettings = OutgoingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(outgoingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(outgoingLinksettings.simpleAuthSettings().get().authToken()).isEqualTo("outgoing-secret");
    assertThat(outgoingLinksettings.url()).isEqualTo(HttpUrl.parse("https://outgoing.example.com"));
  }

  @Test
  public void testWithoutCustomSettings() {
    IlpOverHttpLinkSettings.AuthType authType = IlpOverHttpLinkSettings.AuthType.JWT_HS_256;

    final OutgoingLinkSettings outgoingLinksettings =
        OutgoingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
            .jwtAuthSettings(JwtAuthSettings.builder()
                .tokenIssuer(HttpUrl.parse("https://outgoing-issuer.example.com"))
                .tokenAudience(HttpUrl.parse("https://outgoing-audience.example.com"))
                .tokenSubject("outgoing-subject")
                .tokenExpiry(Duration.ofMillis(30))
                .encryptedTokenSharedSecret("outgoing-credential")
                .build()
            )
            .url(HttpUrl.parse("https://outgoing.example.com"))
            .build();

    assertThat(outgoingLinksettings.authType()).isEqualTo(authType);
    JwtAuthSettings jwtAuthSettings = outgoingLinksettings.jwtAuthSettings().get();
    assertThat(jwtAuthSettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-issuer.example.com"));
    assertThat(jwtAuthSettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://outgoing-audience.example.com"));
    assertThat(jwtAuthSettings.tokenSubject()).isEqualTo("outgoing-subject");
    assertThat(jwtAuthSettings.tokenExpiry().get()).isEqualTo(Duration.ofMillis(30));
    assertThat(jwtAuthSettings.encryptedTokenSharedSecret()).isEqualTo(Optional.of("outgoing-credential"));
    assertThat(outgoingLinksettings.url()).isEqualTo(HttpUrl.parse("https://outgoing.example.com"));
  }
}
