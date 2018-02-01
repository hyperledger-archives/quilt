package org.interledger.node.channels;

import org.interledger.annotations.Immutable;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.node.exceptions.RequestRejectedException;
import org.interledger.node.handlers.ChannelClosedEventHandler;
import org.interledger.node.handlers.ChannelErrorEventHandler;
import org.interledger.node.handlers.ChannelOpenedEventHandler;
import org.interledger.node.handlers.IncomingRequestHandler;
import org.interledger.node.handlers.IncomingTransferEventHandler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockChannel extends AbstractChannel<MockChannel.MockChannelConfig> {

  ExecutorService executor;

  public MockChannel(MockChannelConfig config) {
    super(config);
    executor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void doConnect() {
    //NO OP
  }

  @Override
  public void doDisconnect() {
    //NO OP
  }

  @Override
  public Future<Void> sendTransfer(long amount) {
    return new Future<Void>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return true;
      }

      @Override
      public Void get() throws InterruptedException, ExecutionException {
        return null;
      }

      @Override
      public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
      }
    };
  }


  @Override
  public Future<InterledgerFulfillPacket> sendRequest(InterledgerPreparePacket request) {
    return getConfig().getRequestHandler().onRequest(request);
  }


  /**
   * Simulate an error from the channel
   *
   * @param error
   */
  public void mockError(Exception error) {
    emitError(error);
  }

  /**
   * Simulate an incoming transfer from the channel
   *
   * @param amount
   */
  public void mockIncomingTransfer(long amount) {
    emitIncomingTransfer(amount);
  }

  /**
   * Simulate an incoming request from the channel
   *
   * @param request
   * @return
   * @throws RequestRejectedException
   */
  public InterledgerFulfillPacket mockIncomingRequest(InterledgerPreparePacket request)
      throws RequestRejectedException {
    return dispatchIncomingRequest(request);
  }

  @Immutable
  public abstract static class MockChannelConfig implements ChannelConfig {

    public static MockChannelConfigBuilder builder() {
      return new MockChannelConfigBuilder();
    }

    abstract IncomingRequestHandler getRequestHandler();

  }

}
