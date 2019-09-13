package org.interledger.stream.frames;

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

import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;

import org.immutables.value.Value.Derived;

/**
 * Indicates that the connection was closed.
 */
public interface ConnectionNewAddressFrame extends StreamFrame {

  /**
   * Get the default builder.
   *
   * @return a {@link ConnectionNewAddressFrameBuilder} instance.
   */
  static ConnectionNewAddressFrameBuilder builder() {
    return new ConnectionNewAddressFrameBuilder();
  }

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.ConnectionNewAddress;
  }

  /**
   * The new ILP address of the endpoint that sent the frame.
   *
   * @return A {@link InterledgerAddress} representing the source address of the receiver.
   */
  InterledgerAddress sourceAddress();

  @Immutable
  abstract class AbstractConnectionNewAddressFrame implements ConnectionNewAddressFrame {

    @Derived
    @Override
    public StreamFrameType streamFrameType() {
      return StreamFrameType.ConnectionNewAddress;
    }

  }

}
