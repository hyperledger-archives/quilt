package org.interledger.stream.sender.good;

import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.sender.good.ImmutablePreparePacketComponents.Builder;

import org.immutables.value.Value.Immutable;

import java.util.Optional;
import java.util.function.Supplier;

@Immutable
public interface PreparePacketComponents {

  static Builder builder() {
    return ImmutablePreparePacketComponents.builder();
  }

  /**
   * A {@link Supplier} for the {@link InterledgerPreparePacket} that will be sent. A {@link StreamPacket} can be
   * obtained via {@link InterledgerPacket#typedData()}.
   *
   * @return A {@link Supplier}.
   */
  Supplier<Optional<InterledgerPreparePacket>> preparePacketSupplier();

  PrepareAmounts prepareAmounts();

}
