package org.interledger.transport.psk;

import org.interledger.core.InterledgerAddress;

import java.net.SocketAddress;
import java.util.Optional;

/**
 * Each local Interledger service that provides connectivity out to a peer has an address
 * that can be used to identify it.
 *
 * The {@link InterledgerSocketAddress} object represents that identifier.
 */
public class InterledgerSocketAddress extends SocketAddress implements InterledgerAddress {

  final InterledgerAddress address;

  public static InterledgerSocketAddress of(InterledgerAddress address) {
    return new InterledgerSocketAddress(address);
  }

  public static InterledgerSocketAddress of(String address) {
    return new InterledgerSocketAddress(InterledgerAddress.of(address));
  }

  private InterledgerSocketAddress(InterledgerAddress address) {
    this.address = address;
  }

  @Override
  public String getValue() {
    return address.getValue();
  }

  @Override
  public boolean isLedgerPrefix() {
    return address.isLedgerPrefix();
  }

  @Override
  public boolean startsWith(String addressSegment) {
    return address.startsWith(addressSegment);
  }

  @Override
  public boolean startsWith(InterledgerAddress interledgerAddress) {
    return address.startsWith(interledgerAddress);
  }

  @Override
  public InterledgerAddress with(String addressSegment) {
    return address.with(addressSegment);
  }

  @Override
  public InterledgerAddress getPrefix() {
    return address.getPrefix();
  }

  @Override
  public Optional<InterledgerAddress> getParentPrefix() {
    return address.getParentPrefix();
  }

  @Override
  public boolean hasParentPrefix() {
    return address.hasParentPrefix();
  }

  @Override
  public boolean isRootPrefix() {
    return address.isRootPrefix();
  }
}
