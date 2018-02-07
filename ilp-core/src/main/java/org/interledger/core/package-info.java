/**
 * This package contains classes related to the core Interledger Protocol (ILP) which is the
 * foundational protocol in the overall Interledger suite of protocols. For more details about the
 * ILP core protocol, reference
 * <a href="https://github.com/interledger/rfcs/blob/master/0003-interledger-protocol/0003-interledger-protocol.md">IL-RFC-3</a>.
 *
 * All classes ship a with a default immutable implementation that can be instantiated through the
 * static builder.
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
