package org.interledger.link.spsp;

import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;

import java.util.Optional;

/**
 * An extension of {@link LinkSettings} for Stateless SPSP receiver links.
 */
public interface StatelessSpspReceiverLinkSettings extends LinkSettings {

  static ImmutableStatelessSpspReceiverLinkSettings.Builder builder() {
    return ImmutableStatelessSpspReceiverLinkSettings.builder();
  }

  @Override
  default LinkType getLinkType() {
    return StatelessSpspReceiverLink.LINK_TYPE;
  }

  /**
   * Currency code or other asset identifier that will be used to select the correct rate for this account.
   */
  String assetCode();

  /**
   * Interledger amounts are integers, but most currencies are typically represented as # fractional units, e.g. cents.
   * This property defines how many Interledger units make # up one regular unit. For dollars, this would usually be set
   * to 9, so that Interledger # amounts are expressed in nano-dollars.
   *
   * @return an int representing this account's asset scale.
   */
  int assetScale();

  /**
   * An optionally-present limit on the packet size that this link will fulfill.
   *
   * @return An {@link Optional} instance of {@link UnsignedLong}.
   */
  Optional<UnsignedLong> maxPacketAmount();

  @Value.Immutable
  abstract class AbstractStatelessSpspReceiverLinkSettings implements StatelessSpspReceiverLinkSettings {

    @Derived
    @Override
    public LinkType getLinkType() {
      return StatelessSpspReceiverLink.LINK_TYPE;
    }

  }
}
