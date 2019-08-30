package org.interledger.stream.connection;

import org.interledger.core.Immutable;
import org.interledger.core.InterledgerResponsePacket;

import com.google.common.primitives.UnsignedLong;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Contains information about a pending stream request.
 */
@Immutable
public interface PendingRequest {

  UnsignedLong sequence();

  UnsignedLong amount();

  CompletableFuture<InterledgerResponsePacket> request();
}
