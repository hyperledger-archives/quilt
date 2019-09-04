package org.interledger.link.events;

/**
 * An abstract implementation of {@link LinkEventListener} that no-ops all methods except those relating to {@link
 * LinkEventListener}, which are left to the implementation to define.
 */
public abstract class AbstractLinkEventListener implements LinkEventListener {

  @Override
  public void onConnect(LinkConnectedEvent event) {
    // No-op by default.
  }

  @Override
  public void onDisconnect(LinkDisconnectedEvent event) {
    // No-op by default.
  }

  @Override
  public void onError(LinkErrorEvent event) {
    // No-op by default.
  }
}
