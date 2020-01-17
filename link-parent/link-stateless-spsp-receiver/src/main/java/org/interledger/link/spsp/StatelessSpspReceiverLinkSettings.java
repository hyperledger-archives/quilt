package org.interledger.link.spsp;

import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.exceptions.LinkException;

import org.immutables.value.Value;
import org.immutables.value.Value.Derived;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An extension of {@link LinkSettings} for Stateless SPSP receiver links.
 */
public interface StatelessSpspReceiverLinkSettings extends LinkSettings {

  String ASSET_CODE = "assetCode";
  String ASSET_SCALE = "assetScale";

  static ImmutableStatelessSpspReceiverLinkSettings.Builder builder() {
    return ImmutableStatelessSpspReceiverLinkSettings.builder();
  }

  /**
   * Constructs a new builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param customSettings A {@link Map} of custom settings.
   *
   * @return A {@link ImmutableStatelessSpspReceiverLinkSettings.Builder}.
   */
  static ImmutableStatelessSpspReceiverLinkSettings.Builder fromCustomSettings(
      final Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(customSettings);
    return applyCustomSettings(StatelessSpspReceiverLinkSettings.builder(), customSettings);
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder        A {@link ImmutableStatelessSpspReceiverLinkSettings.Builder} to update with custom settings.
   * @param customSettings A {@link Map} of custom settings.
   *
   * @return A {@link ImmutableStatelessSpspReceiverLinkSettings.Builder}.
   */
  static ImmutableStatelessSpspReceiverLinkSettings.Builder applyCustomSettings(
      final ImmutableStatelessSpspReceiverLinkSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);
    Objects.requireNonNull(customSettings);

    Optional.ofNullable(customSettings.get(ASSET_CODE))
        .map(Object::toString)
        .map(builder::assetCode)
        .orElseThrow(() -> new LinkException(
            "assetCode is required to construct a Link of type " + StatelessSpspReceiverLink.LINK_TYPE,
            LinkId.of("n/a"))
        );

    Optional.ofNullable(customSettings.get(ASSET_SCALE))
        .map(Object::toString)
        .map(Short::parseShort)
        .map(builder::assetScale)
        .orElseThrow(() -> new LinkException(
            "assetScale is required to construct a Link of type " + StatelessSpspReceiverLink.LINK_TYPE,
            LinkId.of("n/a"))
        );

    builder.customSettings(customSettings);

    return builder;
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
  short assetScale();

  @Value.Immutable
  abstract class AbstractStatelessSpspReceiverLinkSettings implements StatelessSpspReceiverLinkSettings {

    @Derived
    @Override
    public LinkType getLinkType() {
      return StatelessSpspReceiverLink.LINK_TYPE;
    }

  }
}
