package org.interledger.link.events;

/**
 * An abstract implementation of {@link LinkConnectionEventListener} that no-ops all methods except those relating to
 * {@link LinkConnectionEventListener}, which are left to the implementation to define.
 */
public abstract class AbstractLinkConnectionEventListener implements LinkConnectionEventListener {

  @Override
  public void onConnect(LinkConnectedEvent event) {
    // No-op by default.
  }

  @Override
  public void onDisconnect(LinkDisconnectedEvent event) {
    // No-op by default.
  }
}
