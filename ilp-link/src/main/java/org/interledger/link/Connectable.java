package org.interledger.link;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * Defines how to connect and disconnect.
 */
public interface Connectable extends Closeable {

  boolean CONNECTED = true;
  boolean NOT_CONNECTED = false;

  /**
   * <p>Connect to the remote peer.</p>
   */
  CompletableFuture<Void> connect();

  /**
   * Disconnect from the remote peer.
   */
  CompletableFuture<Void> disconnect();

  @Override
  default void close() {
    disconnect().join();
  }

  /**
   * <p>Determines if this Connectable is connected or not. If authentication is required, this method
   * will return false until an authenticated session is opened.</p>
   *
   * @return {@code true} if this Connectable is connected; {@code false} otherwise.
   */
  boolean isConnected();
}
