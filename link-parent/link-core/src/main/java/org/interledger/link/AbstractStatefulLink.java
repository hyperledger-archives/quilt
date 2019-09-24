package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.events.LinkConnectedEvent;
import org.interledger.link.events.LinkConnectionEventEmitter;
import org.interledger.link.events.LinkConnectionEventListener;
import org.interledger.link.events.LinkDisconnectedEvent;

import com.google.common.eventbus.EventBus;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * An abstract implementation of a {@link Link} that provides scaffolding for all link implementations.
 */
public abstract class AbstractStatefulLink<L extends LinkSettings>
    extends AbstractLink<L> implements StatefulLink<L> {

  private final AtomicBoolean connected = new AtomicBoolean(NOT_CONNECTED);

  // The emitter used by this link.
  private final LinkConnectionEventEmitter linkConnectionEventEmitter;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier    A supplier for the ILP address of this node operating this Link. This value may
   *                                   be uninitialized, for example, in cases where the Link obtains its address from a
   *                                   parent node using IL-DCP. If an ILP address has not been assigned, or it has not
   *                                   been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   * @param linkSettings               A {@link L} that specified ledger link options.
   * @param linkConnectionEventEmitter A {@link LinkConnectionEventEmitter} that is used to emit events from this link.
   */
  protected AbstractStatefulLink(
      final Supplier<InterledgerAddress> operatorAddressSupplier,
      final L linkSettings,
      final LinkConnectionEventEmitter linkConnectionEventEmitter
  ) {
    super(operatorAddressSupplier, linkSettings);
    this.linkConnectionEventEmitter = Objects.requireNonNull(linkConnectionEventEmitter);
  }

  @Override
  public final CompletableFuture<Void> connect() {
    try {
      if (this.connected.compareAndSet(NOT_CONNECTED, CONNECTED)) {
        logger.debug("[linktype={}] (operatorIlpAddress={}) connecting to linkId={}...",
            this.getLinkSettings().getLinkType(),
            this.operatorAddressAsString(),
            this.getLinkId()
        );

        return this.doConnect()
            .whenComplete(($, error) -> {
              if (error == null) {
                // Emit a connected event...
                this.linkConnectionEventEmitter.emitEvent(LinkConnectedEvent.of(this));

                logger.debug("[linktype={}] (Operator operatorIlpAddress={}) connected to linkId={}",
                    this.getLinkSettings().getLinkType(),
                    this.operatorAddressAsString(),
                    this.getLinkId()
                );
              } else {
                this.connected.set(NOT_CONNECTED);
                final String errorMessage = String.format(
                    "[linktype=%s] (Operator operatorIlpAddress=%s) was unable to connect to linkId=%s",
                    this.getLinkSettings().getLinkType(),
                    this.operatorAddressAsString(),
                    this.getLinkId().value()
                );
                logger.error(errorMessage, error);
              }
            });
      } else {
        logger.debug("[linktype={}] (Operator operatorIlpAddress={})  already connected to linkId={}",
            this.getLinkSettings().getLinkType(),
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
   *
   * @return A {@link CompletableFuture} that completes when the method is completed.
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
            this.getLinkSettings().getLinkType(),
            this.operatorAddressAsString(),
            this.getLinkId()
        );

        return this.doDisconnect()
            .whenComplete(($, error) -> {
              if (error == null) {
                // emit disconnected event.
                this.linkConnectionEventEmitter.emitEvent(LinkDisconnectedEvent.of(this));

                logger.debug("[linktype={}] (ILPAddress={}) disconnected from linkId={}",
                    this.getLinkSettings().getLinkType(),
                    this.operatorAddressAsString(),
                    this.getLinkId()
                );
              } else {
                final String errorMessage =
                    String.format(
                        "[linktype=%s] While trying to disconnect linkId=%s from operatorIlpAddress=%s error=%s",
                        this.getLinkSettings().getLinkType(),
                        this.getLinkId(),
                        this.operatorAddressAsString(),
                        error.getMessage()
                    );
                logger.error(errorMessage, error);
              }
            })
            .thenAccept(($) -> logger.debug("[linktype={}] (ILPAddress={}) disconnected from linkId={}",
                this.getLinkSettings().getLinkType(),
                this.operatorAddressAsString(),
                this.getLinkId()
            ));
      } else {
        logger.debug("[linktype={}] (ILPAddress={}) already disconnected from linkId={}",
            this.getLinkSettings().getLinkType(),
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
   *
   * @return A {@link CompletableFuture} that completes when the method is completed.
   */
  public abstract CompletableFuture<Void> doDisconnect();

  @Override
  public boolean isConnected() {
    return this.connected.get();
  }

  @Override
  public void addLinkEventListener(final LinkConnectionEventListener linkConnectionEventListener) {
    Objects.requireNonNull(linkConnectionEventListener);
    this.linkConnectionEventEmitter.addLinkConnectionEventListener(linkConnectionEventListener);
  }

  @Override
  public void removeLinkEventListener(final LinkConnectionEventListener linkConnectionEventListener) {
    Objects.requireNonNull(linkConnectionEventListener);
    this.linkConnectionEventEmitter.removeLinkConnectionEventListener(linkConnectionEventListener);
  }

  /**
   * A helper method to properly obtain this Node's ILP operating address as a {@link String}, or a consistent value if
   * the address has not yet been set.
   *
   * @return A {@link String}.
   */
  private String operatorAddressAsString() {
    return this.getOperatorAddressSupplier().get().getValue();
  }


  /**
   * An example {@link LinkConnectionEventEmitter} that allows Link-related events to be emitted into an EventBus.
   */
  public static class EventBusConnectionEventEmitter implements LinkConnectionEventEmitter {

    private final EventBus eventBus;

    public EventBusConnectionEventEmitter(final EventBus eventBus) {
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
    public void addLinkConnectionEventListener(final LinkConnectionEventListener linkConnectionEventListener) {
      Objects.requireNonNull(linkConnectionEventListener);
      eventBus.register(linkConnectionEventListener);
    }

    @Override
    public void removeLinkConnectionEventListener(final LinkConnectionEventListener linkConnectionEventListener) {
      Objects.requireNonNull(linkConnectionEventListener);
      eventBus.unregister(linkConnectionEventListener);
    }
  }
}
