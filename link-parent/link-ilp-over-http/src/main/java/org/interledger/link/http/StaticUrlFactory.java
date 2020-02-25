package org.interledger.link.http;

import org.interledger.core.InterledgerAddress;

import okhttp3.HttpUrl;

/**
 * {@link OutgoingUrlFactory} implementation that provides a static URL that does not depend on destination address.
 */
public class StaticUrlFactory implements OutgoingUrlFactory {

  private final HttpUrl outgoingUrl;

  public StaticUrlFactory(HttpUrl outgoingUrl) {
    this.outgoingUrl = outgoingUrl;
  }

  @Override
  public HttpUrl getOutgoingUrl(InterledgerAddress destinationAddress) {
    return outgoingUrl;
  }

}
