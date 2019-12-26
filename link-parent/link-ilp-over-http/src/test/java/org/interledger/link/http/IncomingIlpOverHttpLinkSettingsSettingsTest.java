package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link IncomingLinkSettings}.
 */
public class IncomingIlpOverHttpLinkSettingsSettingsTest extends AbstractHttpLinkSettingsTest {

  // This value doesn't _strictly_ need to be encrypted for purposes of this test. It could easily be plain-text, but
  // for completeness we use the encrypted test-variant.
  private static final String SHH
      = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    IlpOverHttpLinkSettings.AuthType authType = IlpOverHttpLinkSettings.AuthType.JWT_HS_256;
    final Map<String, Object> customSettings = this.customSettingsJwtFlat(authType);
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType()).isEqualTo(authType);
    JwtAuthSettings jwtAuthSettings = incomingLinksettings.jwtAuthSettings().get();
    assertThat(jwtAuthSettings.tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(jwtAuthSettings.tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://incoming-audience.example.com/"));
    assertThat(jwtAuthSettings.encryptedTokenSharedSecret()).isEqualTo(Optional.of("incoming-credential"));
    assertThat(incomingLinksettings.getMinMessageWindow()).isEqualTo(Duration.ofMillis(2500));
  }

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation,
   * ignoring properties not applicable to SIMPLE auth
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotationSimpleAuth() {
    final Map<String, Object> customSettings = this.customSettingsSimpleFlat();
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(incomingLinksettings.simpleAuthSettings().get().authToken()).isEqualTo("incoming-secret");
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHierarchy() {
    IlpOverHttpLinkSettings.AuthType authType = IlpOverHttpLinkSettings.AuthType.JWT_HS_256;
    final Map<String, Object> customSettings = this.customSettingsJwtHierarchical(authType);
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType()).isEqualTo(authType);
    assertThat(incomingLinksettings.jwtAuthSettings().get().tokenIssuer().get())
        .isEqualTo(HttpUrl.parse("https://incoming-issuer.example.com/"));
    assertThat(incomingLinksettings.jwtAuthSettings().get().tokenAudience().get())
        .isEqualTo(HttpUrl.parse("https://incoming-audience.example.com/"));
    assertThat(incomingLinksettings.jwtAuthSettings().get().encryptedTokenSharedSecret())
        .isEqualTo(Optional.of("incoming-credential"));
    assertThat(incomingLinksettings.getMinMessageWindow()).isEqualTo(Duration.ofMillis(2500));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps, ignoring properties not applicable to SIMPLE auth
   */
  @Test
  public void applyCustomSettingsWithMapHierarchySimpleAuth() {
    final Map<String, Object> customSettings = this.customSettingsSimpleHierarchical();
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(incomingLinksettings.simpleAuthSettings().get().authToken()).isEqualTo("incoming-secret");
  }

  @Test
  public void testWithoutCustomSettings() {
    final IncomingLinkSettings incomingLinksettings =
        IncomingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .simpleAuthSettings(SimpleAuthSettings.forAuthToken(SHH))
            .minMessageWindow(Duration.ofMillis(30))
            .build();

    assertThat(incomingLinksettings.authType()).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    assertThat(incomingLinksettings.simpleAuthSettings().get().authToken()).isEqualTo(SHH);
  }
}
