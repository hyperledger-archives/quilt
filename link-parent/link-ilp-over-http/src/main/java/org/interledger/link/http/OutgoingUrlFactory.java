package org.interledger.link.http;

import org.interledger.core.InterledgerAddress;

import okhttp3.HttpUrl;

/**
 * Factory to generate outgoing url given a destination address
 */
public interface OutgoingUrlFactory {
  /**
   * Generates outgoing url
   * @param destinationAddress
   * @return url
   */
  HttpUrl getOutgoingUrl(InterledgerAddress destinationAddress);
}
