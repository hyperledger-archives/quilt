package org.interledger.link.http;

import org.immutables.value.Value;

/**
 * Settings for simple auth scheme
 */
@Value.Immutable
public interface SimpleAuthSettings {

  static ImmutableSimpleAuthSettings forAuthToken(String authToken) {
    return ImmutableSimpleAuthSettings.builder().authToken(authToken).build();
  }

  /**
   * authentication token for authenticating to remote system
   * @return token
   */
  @Value.Redacted
  String authToken();

}
