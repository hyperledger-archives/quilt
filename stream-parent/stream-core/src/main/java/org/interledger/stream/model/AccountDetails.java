package org.interledger.stream.model;

import org.interledger.core.InterledgerAddress;
import org.interledger.fx.Denomination;

import org.immutables.value.Value.Immutable;

import java.util.Optional;

/**
 * The details about an Interledger Account, which is one side of a trustline between two parties.
 */
@Immutable
public interface AccountDetails {

  static ImmutableAccountDetails.Builder builder() {
    return ImmutableAccountDetails.builder();
  }

  /**
   * <p>The ILP address of this account, either statically configured or obtained via IL-DCP.</p>
   *
   * <p>Note that senders are not required to have an ILP address (e.g., to support scenarios where the sender is not
   * routable such as a client making sendMoney requests using an ILP-over-HTTP link that has no incoming URL, perhaps
   * on a non-routable mobile device). In these scenarios, a `private` or otherwise unrouteable scheme should be
   * chosen.</p>
   *
   * @return The optionally-present {@link InterledgerAddress} of this account.
   */
  InterledgerAddress interledgerAddress();

  /**
   * The {@link Denomination} of this account, if known.
   *
   * @return A {@link Denomination}.
   */
  Optional<Denomination> denomination();

}
