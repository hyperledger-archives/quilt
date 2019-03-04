package org.interledger.ildcp;

import org.interledger.annotations.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * An extension of {@link InterledgerPreparePacket} that can be used as an IL-DCP request over Interledger.
 */
public interface IldcpRequestPacket extends InterledgerPreparePacket {

  InterledgerAddress PEER_DOT_CONFIG = InterledgerAddress.of(InterledgerAddressPrefix.PEER.with("config").getValue());

  InterledgerCondition EXECUTION_CONDITION = InterledgerCondition.of(
      Base64.getDecoder().decode("Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=")
  );

  byte[] EMPTY_DATA = new byte[0];

  /**
   * Get the default builder.
   *
   * @return a {@link IldcpRequestPacketBuilder} instance.
   */
  static IldcpRequestPacketBuilder builder() {
    return new IldcpRequestPacketBuilder();
  }

  /**
   * The destination of an ILP packet for IL-DCP is <tt>0</tt> by default, but can be adjusted.
   */
  default BigInteger getAmount() {
    return BigInteger.ZERO;
  }

  /**
   * The execution_condition of an ILP packet for IL-DCP is always
   * <tt>Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=</tt> in Base64 format, which is the SHA-256 hash of a 32-byte
   * array with all 0 values.
   */
  default InterledgerCondition getExecutionCondition() {
    return EXECUTION_CONDITION;
  }

  /**
   * The destination of an ILP packet for IL-DCP is always <tt>peer.config</tt>.
   */
  default InterledgerAddress getDestination() {
    return PEER_DOT_CONFIG;
  }


  default Instant getExpiresAt() {
    return Instant.now().plusSeconds(30);
  }

  /**
   * The data of an ILP packet for IL-DCP is always empty (size: 0).
   */
  default byte[] getData() {
    return EMPTY_DATA;
  }

  /**
   * Exists to satisfy Immutables.
   */
  @Immutable
  abstract class AbstractIldcpRequestPacket implements IldcpRequestPacket {

    @Override
    @Derived
    public InterledgerAddress getDestination() {
      return PEER_DOT_CONFIG;
    }

    @Override
    @Default
    public BigInteger getAmount() {
      return BigInteger.ZERO;
    }

    @Override
    @Derived
    public InterledgerCondition getExecutionCondition() {
      return EXECUTION_CONDITION;
    }

    @Override
    @Default
    public Instant getExpiresAt() {
      return Instant.now().plusSeconds(30);
    }

    @Override
    @Derived
    public byte[] getData() {
      return EMPTY_DATA;
    }

  }

}
