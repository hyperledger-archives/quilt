package org.interledger.ildcp;

import org.interledger.annotations.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;

import org.immutables.value.Value.Derived;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Objects;

/**
 * An extension of {@link InterledgerPreparePacket} that can be used as an IL-DCP request over
 * Interledger.
 */
public interface IldcpRequestPacket extends InterledgerPreparePacket {

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
   * Construct an instance of {@link IldcpRequestPacket} from the supplied {@code ildcpRequest}.
   *
   * @param ildcpRequest An instance of {@link IldcpRequest}.
   *
   * @return An {@link IldcpRequestPacket}.
   */
  static IldcpRequestPacket from(final IldcpRequest ildcpRequest) {
    Objects.requireNonNull(ildcpRequest);
    return IldcpRequestPacket.builder()
        .expiresAt(ildcpRequest.getExpiresAt())
        .build();
  }

  /**
   * The destination of an ILP packet for IL-DCP is always <tt>0</tt>.
   */
  @Derived
  default BigInteger getAmount() {
    return BigInteger.ZERO;
  }

  /**
   * The execution_condition of an ILP packet for IL-DCP is always
   * <tt>Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=</tt> in Base64 format.
   */
  @Derived
  default InterledgerCondition getExecutionCondition() {
    return EXECUTION_CONDITION;
  }

  /**
   * The destination of an ILP packet for IL-DCP is always <tt>peer.config</tt>.
   */
  @Derived
  default InterledgerAddress getDestination() {
    return InterledgerAddress.of("peer.config");
  }

  /**
   * The data of an ILP packet for IL-DCP is always empty (size: 0).
   */
  @Derived
  default byte[] getData() {
    return EMPTY_DATA;
  }

  @Immutable
  abstract class AbstractIldcpRequestPacket implements IldcpRequestPacket {

  }

}
