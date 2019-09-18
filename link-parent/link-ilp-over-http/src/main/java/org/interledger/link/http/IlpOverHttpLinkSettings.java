package org.interledger.link.http;

import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;

import org.immutables.value.Value;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Modifiable;

import java.util.Map;
import java.util.Objects;

/**
 * An extension of {@link LinkSettings} for ILP-over-HTTP links.
 */
public interface IlpOverHttpLinkSettings extends LinkSettings {

  String DOT = ".";
  String ILP_OVER_HTTP = "ilpOverHttp";

  String OUTGOING = "outgoing";
  String INCOMING = "incoming";

  String AUTH_TYPE = "auth_type";

  String TOKEN_ISSUER = "token_issuer";
  String TOKEN_AUDIENCE = "token_audience";
  String TOKEN_SUBJECT = "token_subject";
  String TOKEN_EXPIRY = "token_expiry";

  // Used to grab the auth credential from custom settings...
  String SHARED_SECRET = "shared_secret";
  String URL = "url";

  static ImmutableIlpOverHttpLinkSettings.Builder builder() {
    return ImmutableIlpOverHttpLinkSettings.builder();
  }

  /**
   * Constructs a new builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param customSettings A {@link Map} of custom settings.
   *
   * @return A {@link ImmutableIlpOverHttpLinkSettings.Builder}.
   */
  static ImmutableIlpOverHttpLinkSettings.Builder fromCustomSettings(final Map<String, Object> customSettings) {
    Objects.requireNonNull(customSettings);
    return applyCustomSettings(IlpOverHttpLinkSettings.builder(), customSettings);
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder        A {@link ImmutableIlpOverHttpLinkSettings.Builder} to update with custom settings.
   * @param customSettings A {@link Map} of custom settings.
   *
   * @return A {@link ImmutableIlpOverHttpLinkSettings.Builder}.
   */
  static ImmutableIlpOverHttpLinkSettings.Builder applyCustomSettings(
      final ImmutableIlpOverHttpLinkSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);
    Objects.requireNonNull(customSettings);

    final ImmutableIncomingLinkSettings.Builder incomingLinkSettingsBuilder =
        IncomingLinkSettings.fromCustomSettings(customSettings);
    final ImmutableOutgoingLinkSettings.Builder outgoingLinkSettingsBuilder =
        OutgoingLinkSettings.fromCustomSettings(customSettings);

    builder.incomingHttpLinkSettings(incomingLinkSettingsBuilder.build());
    builder.outgoingHttpLinkSettings(outgoingLinkSettingsBuilder.build());

    builder.customSettings(customSettings);

    return builder;
  }

  @Override
  default LinkType getLinkType() {
    return IlpOverHttpLink.LINK_TYPE;
  }

  /**
   * Link settings for the incoming HTTP link.
   *
   * @return A {@link IncomingLinkSettings}.
   */
  IncomingLinkSettings incomingHttpLinkSettings();

  /**
   * Link settings for the outgoing HTTP link.
   *
   * @return A {@link OutgoingLinkSettings}.
   */
  OutgoingLinkSettings outgoingHttpLinkSettings();

  /**
   * <p>Defines currently-supported ILP-over-HTTP authentication profiles.</p>
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
   */
  enum AuthType {
    /**
     * <p>The actual incoming and outgoing shared-secrets are used as Bearer tokens in an HTTP Authorization
     * header.</p>
     */
    SIMPLE,

    /**
     * Use shared-secret symmetric keys to create and verify JWT_HS_256 tokens.
     */
    JWT_HS_256,

    /**
     * Use RSA asymmetric keys to create aand verify JWT_RS_256 tokens.
     */
    //JWT_RS_256
  }

  @Value.Immutable
  @Modifiable
  abstract class AbstractIlpOverHttpLinkSettings implements IlpOverHttpLinkSettings {

    @Derived
    public LinkType getLinkType() {
      return IlpOverHttpLink.LINK_TYPE;
    }

  }
}
