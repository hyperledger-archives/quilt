package org.interledger.stream.model;

import java.util.Optional;
import org.immutables.value.Value.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.fx.Denomination;

/**
 * The details about an Interledger Account, which is one side of a trustline between two parties.
 */
// TODO: AccountDetails is actually a function of a link, so consider moving this there (or maybe in some core location)? Likewise for Denomination. Think more about this.
@Immutable
public interface AccountDetails {

  static ImmutableAccountDetails.Builder builder() {
    return ImmutableAccountDetails.builder();
  }

  /**
   * The optionally-supplied address of this account. Senders are not required to have an ILP address, especially to
   * support scenarios where the sender is not routable (e.g., client making sendMoney requests using an ILP-over-HTTP
   * link that has no incoming URL, such as from an Android device). Thus, this value is optional.
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
