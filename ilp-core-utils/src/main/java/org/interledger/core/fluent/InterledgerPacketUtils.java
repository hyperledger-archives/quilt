package org.interledger.core.fluent;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.core.AmountTooLargeErrorData;
import org.interledger.core.InterledgerRejectPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Common utilities for operating on ILP packets.
 */
public class InterledgerPacketUtils {

  private static final Logger logger = LoggerFactory.getLogger(InterledgerPacketUtils.class);

  public static Optional<AmountTooLargeErrorData> getAmountTooLargeErrorData(
    final InterledgerRejectPacket rejectPacket
  ) {
    Objects.requireNonNull(rejectPacket);

    // Look in typed-data first. If not found there, try to decode from the data.
    return Optional.ofNullable(rejectPacket.typedData()
      .filter($ -> AmountTooLargeErrorData.class.isAssignableFrom($.getClass()))
      .map($ -> (AmountTooLargeErrorData) $)
      .orElseGet(() -> Optional.ofNullable(rejectPacket.getData())
        .filter(data -> data.length > 0)
        .map(data -> {
          try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            return InterledgerCodecContextFactory.oer().read(AmountTooLargeErrorData.class, bais);
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null; // to manifest as an Optional.empty
          }
        })
        .orElse(null))
    );
  }
}