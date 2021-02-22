package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import org.immutables.value.Value.Default;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension of {@link InterledgerPacket} that indicates a response in an Interledger flow. Per RFC-4, an Interledger
 * prepare packet is sent to a remote counterparty, and the response is either a Reject or a Fulfill. This interface
 * allows both of those response packets to to be handled as one type.
 */
public interface InterledgerResponsePacket extends InterledgerPacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerFulfillPacketBuilder} instance.
   */
  static InterledgerResponsePacketBuilder builder() {
    return new InterledgerResponsePacketBuilder();
  }

  /**
   * Handle this response packet using one of the two supplied functions, depending on this packet's actual type. If
   * this packet is a fulfill packet, then {@code fulfillHandler} will be called. If this packet is a reject packet,
   * then {@code rejectHandler} will be called instead.
   *
   * @param fulfillHandler A {@link Consumer} to call if this packet is an instance of {@link
   *                       InterledgerFulfillPacket}.
   * @param rejectHandler  A {@link Consumer} to call if this packet is an instance of {@link InterledgerRejectPacket}.
   */
  default void handle(
    final Consumer<InterledgerFulfillPacket> fulfillHandler, final Consumer<InterledgerRejectPacket> rejectHandler
  ) {
    Objects.requireNonNull(fulfillHandler);
    Objects.requireNonNull(rejectHandler);

    if (InterledgerFulfillPacket.class.isAssignableFrom(this.getClass())) {
      fulfillHandler.accept((InterledgerFulfillPacket) this);
    } else if (InterledgerRejectPacket.class.isAssignableFrom(this.getClass())) {
      rejectHandler.accept((InterledgerRejectPacket) this);
    } else {
      throw new IllegalStateException(String.format("Unsupported InterledgerResponsePacket Type: %s", this.getClass()));
    }
  }

  /**
   * <p>Handle this response packet using one of the two supplied functions, depending on this packet's actual type. If
   * this packet is a fulfill packet, then {@code fulfillHandler} will be called. If this packet is a reject packet,
   * then {@code rejectHandler} will be called instead.</p>
   *
   * <p>This variant allows for a more fluent style due to returning this object, but is otherwise equivalent to {@link
   * #handle(Consumer, Consumer)}.</p>
   *
   * @param fulfillHandler A {@link Consumer} to call if this packet is an instance of {@link
   *                       InterledgerFulfillPacket}.
   * @param rejectHandler  A {@link Consumer} to call if this packet is an instance of {@link InterledgerRejectPacket}.
   *
   * @return This instance of {@link InterledgerResponsePacket}.
   */
  default InterledgerResponsePacket handleAndReturn(
    final Consumer<InterledgerFulfillPacket> fulfillHandler, final Consumer<InterledgerRejectPacket> rejectHandler
  ) {
    this.handle(fulfillHandler, rejectHandler);
    return this;
  }

  /**
   * Map this packet to another class using one of the two supplied functions, depending on the actual type of this
   * response packet. If this packet is a fulfill packet, then {@code fulfillMapper} will be called. If this packet is a
   * reject packet, then  {@code rejectMapper} will be called instead.
   *
   * @param fulfillMapper A {@link Function} to call if this packet is an instance of {@link InterledgerFulfillPacket}.
   * @param rejectMapper  A {@link Function} to call if this packet is an instance of {@link InterledgerRejectPacket}.
   * @param <R>           The return type of this mapping function.
   *
   * @return An instance of {@code R}.
   */
  default <R> R map(
    final Function<InterledgerFulfillPacket, R> fulfillMapper, final Function<InterledgerRejectPacket, R> rejectMapper
  ) {
    Objects.requireNonNull(fulfillMapper);
    Objects.requireNonNull(rejectMapper);

    if (InterledgerFulfillPacket.class.isAssignableFrom(this.getClass())) {
      return fulfillMapper.apply((InterledgerFulfillPacket) this);
    } else if (InterledgerRejectPacket.class.isAssignableFrom(this.getClass())) {
      return rejectMapper.apply((InterledgerRejectPacket) this);
    } else {
      throw new IllegalStateException(String.format("Unsupported InterledgerResponsePacket Type: %s", this.getClass()));
    }
  }

  /**
   * Map this packet to another class using one of the two supplied functions, depending on the actual type of this
   * response packet. If this packet is a fulfill packet, then {@code fulfillMapper} will be called. If this packet is a
   * reject packet, then  {@code rejectMapper} will be called instead.
   *
   * @param responseMapper A {@link Function} to call if this packet is an instance of {@link
   *                       InterledgerResponsePacket}.
   * @param <R>            The return type of this mapping function.
   *
   * @return An instance of {@code R}.
   */
  default <R> R mapResponse(final Function<InterledgerResponsePacket, R> responseMapper) {
    Objects.requireNonNull(responseMapper);
    return responseMapper.apply(this);
  }

  /**
   * Return a copy of this packet with the supplied {@code typedData} included.
   *
   * @param optTypedData An arbitrary object for the data field.
   *
   * @return A {@link InterledgerResponsePacket}.
   */
  default InterledgerResponsePacket withTypedDataOrThis(final Optional<?> optTypedData) {
    Objects.requireNonNull(optTypedData);
    return optTypedData
      .map(typedData -> this.map(
        // Hydrate the packet with this typed data.
        fulfillPacket -> InterledgerFulfillPacket.builder().from(fulfillPacket).typedData(typedData).build(),
        rejectPacket -> InterledgerRejectPacket.builder().from(rejectPacket).typedData(typedData).build()
      ))
      .orElse(this); // <-- Return this packet without typedData if `typedData` is null
  }

  @Immutable
  abstract class AbstractInterledgerResponsePacket implements InterledgerResponsePacket {

    @Override
    @Default
    public byte[] getData() {
      return new byte[0];
    }

    /**
     * Prints the immutable value {@code InterledgerFulfillPacket} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "InterledgerResponsePacket{"
        + ", data=" + Base64.getEncoder().encodeToString(getData())
        + ", typedData=" + typedData().orElse("n/a")
        + "}";
    }
  }

}
