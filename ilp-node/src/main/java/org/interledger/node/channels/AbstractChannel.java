package org.interledger.node.channels;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRuntimeException;
import org.interledger.node.events.ChannelClosedEvent;
import org.interledger.node.events.ChannelErrorEvent;
import org.interledger.node.events.ChannelOpenedEvent;
import org.interledger.node.events.IncomingTransferEvent;
import org.interledger.node.exceptions.RequestRejectedException;
import org.interledger.node.handlers.ChannelClosedEventHandler;
import org.interledger.node.handlers.ChannelErrorEventHandler;
import org.interledger.node.handlers.ChannelOpenedEventHandler;
import org.interledger.node.handlers.IncomingRequestHandler;
import org.interledger.node.handlers.IncomingTransferEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstract implementation of a {@link Channel} that directly connects emitted
 * events to proper handlers.
 */
public abstract class AbstractChannel<T extends ChannelConfig> implements Channel {

  private static final boolean OPEN = true;
  private static final boolean CLOSED = false;

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  /**
   * A typed representation of the configuration options passed-into this channels.
   */
  private final T config;

  private ChannelOpenedEventHandler openedEventHandler;
  private ChannelClosedEventHandler closedEventHandler;
  private ChannelErrorEventHandler errorEventHandler;
  private IncomingTransferEventHandler transferEventHandler;
  private IncomingRequestHandler requestEventHandler;

  private AtomicBoolean open = new AtomicBoolean(false);

  /**
   * Required-args Constructor.
   *
   * @param config A {@link T} that specified channels options.
   */
  protected AbstractChannel(final T config) {
    this.config = Objects.requireNonNull(config);
  }

  @Override
  public final void open() {
    logger.info("open channel: {}", this.getConfig());

    try {
      if (!this.isOpen()) {
        this.doConnect();
        this.open.compareAndSet(CLOSED, OPEN);
        this.openedEventHandler.onOpen(
            ChannelOpenedEvent.builder()
                .build()
        );
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);

      // If we can't connect, this will trigger the removal of this ledger channels.
      this.errorEventHandler.onError(
          ChannelErrorEvent.builder()
              .error(e).build()
      );
    }
  }

  public abstract void doConnect();

  @Override
  public final void close() {
    logger.info("disconnect {}", this.getConfig());

    if (this.isOpen()) {
      this.doDisconnect();
      this.open.compareAndSet(OPEN, CLOSED);
      this.closedEventHandler.onDisconnect(
          ChannelClosedEvent.builder()
              .build()
      );
    }
  }

  public abstract void doDisconnect();

  /**
   * Query whether the channel is currently open.
   *
   * @return {@code true} if the channel is open, {@code false} otherwise.
   */
  @Override
  public boolean isOpen() {
    return this.open.get();
  }

  @Override
  public final Channel setOnOpened(ChannelOpenedEventHandler eventHandler) {
    openedEventHandler = eventHandler;
    return this;
  }

  @Override
  public final Channel setOnClosed(ChannelClosedEventHandler eventHandler) {
    closedEventHandler = eventHandler;
    return this;
  }

  @Override
  public final Channel setOnError(ChannelErrorEventHandler eventHandler) {
    errorEventHandler = eventHandler;
    return this;
  }

  @Override
  public final Channel setOnIncomingTransfer(IncomingTransferEventHandler eventHandler) {
    transferEventHandler = eventHandler;
    return this;
  }

  @Override
  public final Channel setIncomingRequestHandler(IncomingRequestHandler eventHandler) {
    requestEventHandler = eventHandler;
    return this;
  }

  protected final void emitError(Exception error) {
    errorEventHandler.onError(
        ChannelErrorEvent.builder()
          .error(error)
          .build()
    );
  }

  protected final void emitIncomingTransfer(long amount) {
    transferEventHandler.onTransfer(
        IncomingTransferEvent.builder()
        .transferAmount(amount)
        .build()
    );
  }

  protected final InterledgerFulfillPacket dispatchIncomingRequest(
      InterledgerPreparePacket request) throws RequestRejectedException {
    try {
      return requestEventHandler.onRequest(request).get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RequestRejectedException) {
        throw (RequestRejectedException) e.getCause();
      }
      throw new InterledgerRuntimeException("Error processing request.", e.getCause());
    } catch (InterruptedException e) {
      throw new InterledgerRuntimeException("InterruptedException while processing request.", e);
    }
  }

  protected final Future<InterledgerFulfillPacket> dispatchIncomingRequestAsync(
      InterledgerPreparePacket request) {
      return requestEventHandler.onRequest(request);
  }

  protected T getConfig() {
    return this.config;
  }

}
