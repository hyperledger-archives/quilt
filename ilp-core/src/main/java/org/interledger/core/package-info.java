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
/**
 * <p>This package contains classes related to the core Interledger Protocol (ILP) which is the foundational protocol
 * in the overall Interledger suite of protocols. For more details about the ILP core protocol, reference
 * <a href="https://github.com/interledger/rfcs/blob/master/0003-interledger-protocol/0003-interledger-protocol.md">
 * IL-RFC-3</a></p>.
 *
 * <p>All classes ship a with a default immutable implementation that can be instantiated through the static
 * builder.</p>
 *
 * <pre>
 *  {@code
 *  InterledgerPreparePacket payment = InterledgerPreparePacket.builder()
 *      .destinationAccount(InterledgerAddress.of("g.crypto.xrp.r982734982367498"))
 *      .data()
 *  }
 * </pre>
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0003-interledger-protocol"
 */
package org.interledger.core;
