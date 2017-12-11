package org.interledger.codecs;

import org.interledger.InterledgerPacket;
import org.interledger.InterledgerPacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link InterledgerPacket}
 * having a discrete {@link InterledgerPacketType} and data payload.
 */
public interface InterledgerPacketCodec<T extends InterledgerPacket> extends Codec<T> {

  /**
   * Accessor for the {@link InterledgerPacketType} of this {@link Codec}.
   *
   * @return The {@link InterledgerPacketType} instance.
   */
  InterledgerPacketType getTypeId();
}
