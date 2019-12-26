package org.interledger.link.http;

import java.util.Optional;

/**
 * Common auth settings for incoming and outgoing links.
 */
public interface AuthenticatedLinkSettings {

  /**
   * Auth settings if using SIMPLE scheme.
   *
   * @return settings
   */
  Optional<SimpleAuthSettings> simpleAuthSettings();

  /**
   * Auth settings if using JWT scheme.
   *
   * @return settings
   */
  Optional<JwtAuthSettings> jwtAuthSettings();


  /**
   * The type of Auth to support for this link.
   *
   * @return A {@link IlpOverHttpLinkSettings.AuthType} for this link
   */
  IlpOverHttpLinkSettings.AuthType authType();

}
