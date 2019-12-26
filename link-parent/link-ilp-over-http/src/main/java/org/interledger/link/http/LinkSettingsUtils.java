package org.interledger.link.http;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

/**
 * Utility methods for the customSettings Map objects used by {@link IlpOverHttpLinkSettings}
 */
public class LinkSettingsUtils {

  /**
   * Flattens a {@code Map<String, Object || Map<String, Object>>} to a {@code Map<String, Object>} such that
   * a map with values {@code ["foo" -> [ "bar" -> "baz", "fizz" -> "buzz" ] } would become
   * {@code ["foo.bar" -> "baz" , "foo.fizz" -> "buzz" ] }. This is to deal with differences between Java property parsers
   * that generate hierarchical Map of Maps (where keys are single segments of a property name) instead a flattened
   * Map where all keys are the full property name.
   *
   * @param settings map to flatten
   * @return flattened map
   */
  public static Map<String, Object> flattenSettings(Map<String, Object> settings) {
    return flattenSettings("", settings);
  }

  private static Map<String, Object> flattenSettings(String prefix, Map<String, Object> settings) {
    ImmutableMap.Builder<String, Object> flattened = ImmutableMap.builder();
    settings.forEach((key, value) -> {
      if (value instanceof Map) {
        try {
          Map<String, Object> typedValue = (Map<String, Object>) value;
          flattened.putAll(flattenSettings(prefix + key + ".", typedValue));
        } catch (ClassCastException e) {
          throw new IllegalArgumentException("Found value with a key that is not a String");
        }
      } else {
        flattened.put(prefix + key, value);
      }
    });
    return flattened.build();
  }

  /**
   * Determines the incoming {@link org.interledger.link.http.IlpOverHttpLinkSettings.AuthType}
   * from custom settings values.
   *
   * @param customSettings
   * @return auth type or null if no auth settings founds
   */
  public static Optional<IlpOverHttpLinkSettings.AuthType> getIncomingAuthType(Map<String, Object> customSettings) {
    return Optional.ofNullable(flattenSettings(customSettings).get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE))
        .map(Object::toString)
        .map(String::toUpperCase)
        .map(IlpOverHttpLinkSettings.AuthType::valueOf);
  }

  /**
   * Determines the outgoing {@link org.interledger.link.http.IlpOverHttpLinkSettings.AuthType}
   * from custom settings values.
   *
   * @param customSettings
   * @return auth type or null if no auth settings founds
   */
  public static Optional<IlpOverHttpLinkSettings.AuthType> getOutgoingAuthType(Map<String, Object> customSettings) {
    return Optional.ofNullable(flattenSettings(customSettings).get(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE))
        .map(Object::toString)
        .map(String::toUpperCase)
        .map(IlpOverHttpLinkSettings.AuthType::valueOf);
  }

  public static void validate(AuthenticatedLinkSettings linkSettings) {
    switch (linkSettings.authType()) {
      case SIMPLE: {
        Preconditions.checkArgument(linkSettings.simpleAuthSettings().isPresent(), "simpleAuthSettings required");
        break;
      }
      case JWT_HS_256: {
        Preconditions.checkArgument(linkSettings.jwtAuthSettings().isPresent(), "jwtAuthSettings required");
        JwtAuthSettings jwtAuthSettings = linkSettings.jwtAuthSettings().get();
        Preconditions.checkArgument(jwtAuthSettings.encryptedTokenSharedSecret().isPresent(),
            "encryptedTokenSharedSecret required for HS256");
        break;
      }
      case JWT_RS_256:
      {
        Preconditions.checkArgument(linkSettings.jwtAuthSettings().isPresent(), "jwtAuthSettings required");
        JwtAuthSettings jwtAuthSettings = linkSettings.jwtAuthSettings().get();
        Preconditions.checkArgument(jwtAuthSettings.tokenAudience().isPresent(),
            "tokenAudience required for RS256");
        Preconditions.checkArgument(jwtAuthSettings.tokenIssuer().isPresent(),
            "tokenIssuer required for RS256");
        break;
      }
      default:
        throw new IllegalStateException("unknown authType " + linkSettings.authType());
    }

  }

}
