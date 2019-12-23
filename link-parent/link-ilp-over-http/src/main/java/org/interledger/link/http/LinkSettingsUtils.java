package org.interledger.link.http;

import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TOKEN;
import static org.interledger.link.http.IlpOverHttpLinkSettings.DOT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.JWT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SIMPLE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_SUBJECT;

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
        }
        catch (ClassCastException e) {
          throw new IllegalArgumentException("Found value with a key that is not a String");
        }
      }
      else {
        flattened.put(prefix + key, value);
      }
    });
    return flattened.build();
  }

  /**
   * Determines the {@link org.interledger.link.http.IlpOverHttpLinkSettings.AuthType} from custom settings values.
   * @param customSettings
   * @return auth type or null if no auth settings founds
   */
  public static Optional<IlpOverHttpLinkSettings.AuthType> getAuthType(Map<String, Object> customSettings) {
    Map<String, Object> flattenedSettings = flattenSettings(customSettings);
    boolean hasSimpleSettings = containsKeyWithPrefix(flattenedSettings, SIMPLE + DOT + AUTH_TOKEN);
    boolean hasJwtSettings = containsKeyWithPrefix(flattenedSettings, JWT + DOT + TOKEN_SUBJECT);
    if (hasSimpleSettings) {
      if (hasJwtSettings) {
        throw new IllegalArgumentException("customSettings cannot contain both simple and jwt auth settings");
      }
      return Optional.of(IlpOverHttpLinkSettings.AuthType.SIMPLE);
    }
    else if (hasJwtSettings) {
      return Optional.of(IlpOverHttpLinkSettings.AuthType.JWT);
    }
    else {
      return Optional.empty();
    }
  }

  private static boolean containsKeyWithPrefix(Map<String, Object> settings, String prefix) {
    return settings.keySet().stream().anyMatch(key -> key.contains(prefix));
  }

}
