package org.interledger.transport.psk;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.TimeUnit;

/**
 * A PskSocketGroup should group sockets from the same ILP channels.
 *
 * The group gives the PskReceivingSocket access to the channels to get its local ILP address prefix
 */
public class PskSocketGroup extends AsynchronousChannelGroup {

  /**
   * Initialize a new instance of this class.
   *
   * @param provider The asynchronous channel provider for this group
   */
  protected PskSocketGroup(AsynchronousChannelProvider provider) {
    super(provider);
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public void shutdown() {

  }

  @Override
  public void shutdownNow() throws IOException {

  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return false;
  }
}
