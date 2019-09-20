package org.interledger.link;

import org.interledger.core.InterledgerAddress;
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
public abstract class AbstractLink<L extends LinkSettings> implements Link<L> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * The optionally-present {@link InterledgerAddress} of the operator of this sender. Sometimes the Operator Address is
   * not yet populated when this client is constructed (e.g, IL-DCP). Thus, the value is optional to accommodate these
   * cases.
   */
  private final Supplier<InterledgerAddress> operatorAddressSupplier;

  /**
   * A typed representation of the configuration options passed-into this ledger link.
   */
  private final L linkSettings;

  private final AtomicReference<LinkHandler> linkHandlerAtomicReference = new AtomicReference<>();

  // Non-final for late-binding...
  private final AtomicReference<LinkId> linkId = new AtomicReference<>();

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A supplier for the ILP address of this node operating this Link. This value may be
   *                                uninitialized, for example, in cases where the Link obtains its address from a
   *                                parent node using IL-DCP. If an ILP address has not been assigned, or it has not
   *                                been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   * @param linkSettings            A {@link L} that specified ledger link options.
   */
  protected AbstractLink(
      final Supplier<InterledgerAddress> operatorAddressSupplier,
      final L linkSettings
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.linkSettings = Objects.requireNonNull(linkSettings);
  }

  @Override
  public LinkId getLinkId() {
    if (linkId.get() == null) {
      throw new IllegalStateException("The LinkId must be set before using a Link");
    }
    return linkId.get();
  }

  @Override
  public void setLinkId(final LinkId linkId) {
    if (!this.linkId.compareAndSet(null, Objects.requireNonNull(linkId))) {
      throw new IllegalStateException("LinkId may only be set once");
    }
  }

  @Override
  public Supplier<InterledgerAddress> getOperatorAddressSupplier() {
    return operatorAddressSupplier;
  }

  @Override
  public L getLinkSettings() {
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
