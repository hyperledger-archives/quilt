package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.events.LinkConnectedEvent;
import org.interledger.link.events.LinkDisconnectedEvent;
import org.interledger.link.events.LinkErrorEvent;
import org.interledger.link.events.LinkEventEmitter;
import org.interledger.link.events.LinkEventListener;
import org.interledger.link.exceptions.LinkHandlerAlreadyRegisteredException;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * An abstract implementation of a {@link Link} that provides scaffolding for all link implementations.
 */
public abstract class AbstractLink<LS extends LinkSettings> implements Link<LS> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Optional to allow for IL-DCP
  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;

  /**
   * A typed representation of the configuration options passed-into this ledger link.
   */
  private final LS linkSettings;
  private final AtomicBoolean connected = new AtomicBoolean(NOT_CONNECTED);

  private final AtomicReference<LinkHandler> linkHandlerAtomicReference = new AtomicReference<>();

  // The emitter used by this link.
  private final LinkEventEmitter linkEventEmitter;

  // Non-final for late-binding...
  private LinkId linkId;

  /**
   * Required-args Constructor.
   *
   * @param linkSettings     A {@link LS} that specified ledger link options.
   * @param linkEventEmitter A {@link LinkEventEmitter} that is used to emit events from this link.
   */
  protected AbstractLink(
      final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
      final LS linkSettings,
      final LinkEventEmitter linkEventEmitter
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.linkSettings = Objects.requireNonNull(linkSettings);
    this.linkEventEmitter = Objects.requireNonNull(linkEventEmitter);
  }

  @Override
  public LinkId getLinkId() {
    if (linkId == null) {
      throw new IllegalStateException("The LinkId must be set before using a Link");
    }
    return linkId;
  }

  /**
   * Allows the linkId to be set after construction. This is important because often times a LinkId is not known until
   * after it is constructed (i.e., it cannot be discerned from {@link LinkSettings}).
   *
   * @param linkId
   */
  public void setLinkId(final LinkId linkId) {
    if (this.linkId == null) {
      this.linkId = Objects.requireNonNull(linkId);
    } else {
      throw new IllegalStateException("LinkId may only be set once");
    }
  }

  @Override
  public Supplier<Optional<InterledgerAddress>> getOperatorAddressSupplier() {
    return operatorAddressSupplier;
  }

  @Override
  public LS getLinkSettings() {
    return this.linkSettings;
  }

  @Override
  public final CompletableFuture<Void> connect() {
    try {
      if (this.connected.compareAndSet(NOT_CONNECTED, CONNECTED)) {
        logger.debug("[linktype={}] (operatorIlpAddress={}) connecting to linkId={}...",
            this.linkSettings.getLinkType(),
            this.operatorAddressAsString(),
            this.getLinkId()
        );

        return this.doConnect()
            .whenComplete(($, error) -> {
              if (error == null) {
                // Emit a connected event...
                this.linkEventEmitter.emitEvent(LinkConnectedEvent.of(this));

                logger.debug("[linktype={}] (Operator operatorIlpAddress={}) connected to linkId={}",
                    this.linkSettings.getLinkType(),
                    this.operatorAddressAsString(),
                    this.getLinkId()
                );
              } else {
                this.connected.set(NOT_CONNECTED);
                final String errorMessage = String.format(
                    "[linktype={}] (Operator operatorIlpAddress={}) was unable to connect to linkId={}",
                    this.linkSettings.getLinkType(),
                    this.operatorAddressAsString(),
                    this.getLinkId().value()
                );
                logger.error(errorMessage, error);
                this.linkEventEmitter.emitEvent(LinkErrorEvent.of(this, error));
              }
            });
      } else {
        logger.debug("[linktype={}] (Operator operatorIlpAddress={})  already connected to linkId={}",
            this.linkSettings.getLinkType(),
            this.operatorAddressAsString(),
            this.getLinkId()
        );
        // No-op: We're already connected.
        return CompletableFuture.completedFuture(null);
      }
    } catch (RuntimeException e) {
      // If we can't connect, then disconnect this account in order to trigger any listeners.
      this.disconnect().join();
      throw e;
    } catch (Exception e) {
      // If we can't connect, then disconnect this account in order to trigger any listeners.
      this.disconnect().join();
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Perform the logic of actually connecting to the remote peer.
   */
  public abstract CompletableFuture<Void> doConnect();

  @Override
  public void close() {
    this.disconnect().join();
  }

  @Override
  public final CompletableFuture<Void> disconnect() {
    try {
      if (this.connected.compareAndSet(CONNECTED, NOT_CONNECTED)) {
        logger.debug("[linktype={}] (ILPAddress={}) disconnecting from linkId={}...",
            this.linkSettings.getLinkType(),
            this.operatorAddressAsString(),
            this.getLinkId()
        );

        return this.doDisconnect()
            .whenComplete(($, error) -> {
              if (error == null) {
                // emit disconnected event.
                this.linkEventEmitter.emitEvent(LinkDisconnectedEvent.of(this));

                logger.debug("[linktype={}] (ILPAddress={}) disconnected from linkId={}",
                    this.linkSettings.getLinkType(),
                    this.operatorAddressAsString(),
                    this.getLinkId()
                );
              } else {
                final String errorMessage =
                    String.format(
                        "[linktype=%s] While trying to disconnect linkId=%s from operatorIlpAddress=%s error=%s",
                        this.linkSettings.getLinkType(),
                        this.getLinkId(),
                        this.operatorAddressAsString(),
                        error.getMessage()
                    );
                logger.error(errorMessage, error);
              }
            })
            .thenAccept(($) -> logger.debug("[linktype={}] (ILPAddress={}) disconnected from linkId={}",
                this.linkSettings.getLinkType(),
                this.operatorAddressAsString(),
                this.getLinkId()
            ));
      } else {
        logger.debug("[linktype={}] (ILPAddress={}) already disconnected from linkId={}",
            this.linkSettings.getLinkType(),
            this.operatorAddressAsString(),
            this.getLinkId());
        // No-op: We're already disconnected.
        return CompletableFuture.completedFuture(null);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Perform the logic of disconnecting from the remote peer.
   */
  public abstract CompletableFuture<Void> doDisconnect();

  @Override
  public boolean isConnected() {
    return this.connected.get();
  }

  @Override
  public void registerLinkHandler(final LinkHandler ilpDataHandler)
      throws LinkHandlerAlreadyRegisteredException {
    Objects.requireNonNull(ilpDataHandler, "ilpDataHandler must not be null!");
    if (!this.linkHandlerAtomicReference.compareAndSet(null, ilpDataHandler)) {
      throw new LinkHandlerAlreadyRegisteredException(
          "DataHandler may not be registered twice. Call unregisterDataHandler first!",
          this.getLinkId()
      );
    }
  }

  @Override
  public void unregisterLinkHandler() {
    this.linkHandlerAtomicReference.set(null);
  }

  @Override
  public Optional<LinkHandler> getLinkHandler() {
    return Optional.ofNullable(linkHandlerAtomicReference.get());
  }

  @Override
  public void addLinkEventListener(final LinkEventListener linkEventListener) {
    Objects.requireNonNull(linkEventListener);
    this.linkEventEmitter.addLinkEventListener(linkEventListener);
  }

  @Override
  public void removeLinkEventListener(final LinkEventListener linkEventListener) {
    Objects.requireNonNull(linkEventListener);
    this.linkEventEmitter.removeLinkEventListener(linkEventListener);
  }

  /**
   * A helper method to properly obtain this Node's ILP operating address as a {@link String}, or a consistent value if
   * the address has not yet been set.
   *
   * @return A {@link String}.
   */
  private final String operatorAddressAsString() {
    return this.operatorAddressSupplier.get().map(InterledgerAddress::getValue).orElse("unset");
  }

  /**
   * An example {@link LinkEventEmitter} that allows Link-related events to be emitted into an EventBus.
   */
  public static class EventBusEventEmitter implements LinkEventEmitter {

    private final EventBus eventBus;

    public EventBusEventEmitter(final EventBus eventBus) {
      this.eventBus = Objects.requireNonNull(eventBus);
    }

    /////////////////
    // Event Emitters
    /////////////////

    @Override
    public void emitEvent(final LinkConnectedEvent event) {
      eventBus.post(event);
    }

    @Override
    public void emitEvent(final LinkDisconnectedEvent event) {
      eventBus.post(event);
    }

    @Override
    public void emitEvent(final LinkErrorEvent event) {
      eventBus.post(event);
    }

    @Override
    public void addLinkEventListener(final LinkEventListener linkEventListener) {
      Objects.requireNonNull(linkEventListener);
      eventBus.register(linkEventListener);
    }

    @Override
    public void removeLinkEventListener(final LinkEventListener linkEventListener) {
      Objects.requireNonNull(linkEventListener);
      eventBus.unregister(linkEventListener);
    }
  }
}
