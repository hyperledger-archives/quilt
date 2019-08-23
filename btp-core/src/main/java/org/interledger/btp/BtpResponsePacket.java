package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The parent interface for all BTP response packets.
 */
public interface BtpResponsePacket extends BtpPacket {

  /**
   * Handle this BTP packet using one of the supplied functions, depending on this packet's actual type.
   *
   * @param btpResponseHandler A {@link Consumer} to call if this packet is an instance of {@link BtpResponse}.
   * @param btpErrorHandler    A {@link Consumer} to call if this packet is an instance of {@link BtpError}.
   */
  default void handle(final Consumer<BtpResponse> btpResponseHandler, final Consumer<BtpError> btpErrorHandler
  ) {
    Objects.requireNonNull(btpResponseHandler);
    Objects.requireNonNull(btpErrorHandler);

    switch (this.getType()) {
      case RESPONSE: {
        btpResponseHandler.accept((BtpResponse) this);
        return;
      }
      case ERROR: {
        btpErrorHandler.accept((BtpError) this);
        return;
      }
      default: {
        throw new RuntimeException(String.format("Unsupported BtpPacket Type: %s", this.getType()));
      }
    }
  }

  /**
   * <p>Handle this BTP packet using one of the supplied functions, depending on this packet's actual type.</p>
   *
   * <p>This variant allows for a more fluent style due to returning this object, but is otherwise equivalent to {@link
   * #handle(Consumer, Consumer)}.</p>
   *
   * @param btpResponseHandler A {@link Consumer} to call if this packet is an instance of {@link BtpResponse}.
   * @param btpErrorHandler    A {@link Consumer} to call if this packet is an instance of {@link BtpError}.
   *
   * @return This instance of {@link BtpResponsePacket}.
   */
  default BtpResponsePacket handleAndReturn(
      final Consumer<BtpResponse> btpResponseHandler,
      final Consumer<BtpError> btpErrorHandler
  ) {
    this.handle(btpResponseHandler, btpErrorHandler);
    return this;
  }

  /**
   * Map this packet to another class using one of the two supplied functions, depending on the actual type of this
   * response packet. If this packet is a BTP response packet (i.e., an ILPv4 fulfill packet), then {@code
   * btpResponseMapper} will be called. If this packet is a BTP error packet (i.e., an ILPv4 reject packet), then
   * {@code rejectMapper} will be called instead.
   *
   * @param btpResponseMapper A {@link Function} to call if this packet is an instance of {@link BtpResponse}.
   * @param btpErrorMapper    A {@link Function} to call if this packet is an instance of {@link BtpError}.
   * @param <R>               The return type of this mapping function.
   *
   * @return An instance of {@link R}.
   */
  default <R> R map(final Function<BtpResponse, R> btpResponseMapper, final Function<BtpError, R> btpErrorMapper
  ) {
    Objects.requireNonNull(btpResponseMapper);
    Objects.requireNonNull(btpErrorMapper);

    switch (this.getType()) {
      case RESPONSE: {
        return btpResponseMapper.apply((BtpResponse) this);
      }
      case ERROR: {
        return btpErrorMapper.apply((BtpError) this);
      }
      default: {
        throw new RuntimeException(String.format("Unsupported BtpPacket Type: %s", this.getType()));
      }
    }
  }

}
