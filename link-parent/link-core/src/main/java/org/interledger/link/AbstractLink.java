package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.events.LinkConnectionEventEmitter;
import org.interledger.link.exceptions.LinkHandlerAlreadyRegisteredException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
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

  private final AtomicReference<LinkHandler> linkHandlerAtomicReference = new AtomicReference<>();

  // Non-final for late-binding...
  private LinkId linkId;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} that supplies the optionally-present operator address of the node
   *                                operating this Link (note this is optional to support IL-DCP).
   * @param linkSettings            A {@link LS} that specified ledger link options.
   */
  protected AbstractLink(
      final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
      final LS linkSettings
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.linkSettings = Objects.requireNonNull(linkSettings);
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
}
