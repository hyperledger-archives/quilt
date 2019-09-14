package org.interledger.link;

import org.interledger.core.InterledgerAddress;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A factory for constructing instances of {@link Link} based upon configured settings.
 */
public interface LinkFactory {

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @param operatorAddressSupplier A {@link Supplier} that supplies the ILP address of the Connector operating this
   *                                link.
   * @param linkSettings            An instance of {@link LinkSettings} to initialize this link from.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  Link<?> constructLink(Supplier<Optional<InterledgerAddress>> operatorAddressSupplier, LinkSettings linkSettings);

  /**
   * Helper method to apply custom settings on a per-link-type basis.
   *
   * @param linkSettings A {@link LinkSettings} to apply to this instance.
   *
   * @return A {@link LinkSettings} with applied custom settings.
   */
  default LinkSettings applyCustomSettings(LinkSettings linkSettings) {
    return linkSettings;
  }

  /**
   * Determines if this factory support a particular type of {@link LinkType}.
   *
   * @param linkType A {@link LinkType} to check compatibility for.
   *
   * @return {@code true} if this factory supports the specified linkType; {@code false} otherwise.
   */
  boolean supports(LinkType linkType);

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @param $                       A {@link Class} to satisfy Java generics.
   * @param operatorAddressSupplier A {@link Supplier} for the address of the node operating this factory.
   * @param linkSettings            A {@link LinkSettings} to use in order to construct a {@link Link}.
   * @param <LS>                    A type that extends {@link LinkSettings}.
   * @param <L>                     A type that extends {@link Link}.
   *
   * @return An instance of {@link L}.
   */
  default <LS extends LinkSettings, L extends Link<LS>> L constructLink(
      final Class<L> $, Supplier<Optional<InterledgerAddress>> operatorAddressSupplier, final LS linkSettings
  ) {
    return (L) this.constructLink(operatorAddressSupplier, linkSettings);
  }
}
