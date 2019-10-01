package org.interledger.stream;

import org.interledger.stream.ImmutableDenomination.Builder;

import org.immutables.value.Value.Immutable;

/**
 * Defines an asset denomination by providing an asset code and scale.
 */
@Immutable
public interface Denomination {

  static Builder builder() {
    return ImmutableDenomination.builder();
  }

  /**
   * Currency code or other asset identifier. For example, `USD`, `EUR`, or `BTC`.
   *
   * @return A {@link String} containing the asset code.
   */
  String assetCode();

  /**
   * <p>An asset scale is the difference, in orders of magnitude, between an asset's `standard unit` and a
   * corresponding `fractional unit`.</p>
   *
   * <p>A standard unit represents the typical unit of account for a particular asset. For example 1 USD in the case of
   * U.S. dollars, or 1 BTC in the case of Bitcoin (Note that peers are free to define this value in any way, but
   * participants in an Interledger accounting relationship must be sure to use the same value. Thus, it is suggested to
   * use typical values when possible).</p>
   *
   * <p>A fractional unit represents some unit smaller than its corresponding standard unit, but with greater
   * precision. Examples of fractional monetary units include one cent ($0.01 USD), or 1 satoshi (0.00000001 BTC).</p>
   *
   * <p>Because Interledger amounts are integers, but most currencies are typically represented as fractional units
   * (e.g. cents), this property defines how many Interledger units make up one standard unit of the asset code
   * specified above.</p>
   *
   * <p>More formally, the asset scale is a non-negative integer (0, 1, 2, …) such that one standard unit equals
   * 10^(-scale) of a corresponding fractional unit. If the fractional unit equals the standard unit, then the asset
   * scale is 0.</p>
   *
   * <p>For example, one "cent" represents an asset scale of 2 in the case of USD; 1 satoshi represents an asset scale
   * of 8 in the case of Bitcoin; and 1 drop represents an asset scale of 6 in XRP.</p>
   *
   * @return A {@link Short} representing the asset scale.
   */
  short assetScale();
}
