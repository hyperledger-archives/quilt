package org.interledger.core;

import org.interledger.annotations.Immutable;
import org.interledger.cryptoconditions.Fulfillment;

import java.util.Arrays;

public interface InterledgerFulfillPacket extends InterledgerPacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerFulfillPacketBuilder} instance.
   */
  static InterledgerFulfillPacketBuilder builder() {
    return new InterledgerFulfillPacketBuilder();
  }

  Fulfillment getFulfillment();

  /**
   * Arbitrary data for the sender that is set by the transport layer of a payment (for example,
   * this may contain PSK data).
   *
   * @return A byte array.
   */
  byte[] getData();

  @Immutable
  abstract class AbstractInterledgerFulfillPacket implements InterledgerFulfillPacket {

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      InterledgerFulfillPacket impl = (InterledgerFulfillPacket) obj;

      return getFulfillment().equals(impl.getFulfillment())
          && Arrays.equals(getData(), impl.getData());
    }

    @Override
    public int hashCode() {
      int result = getFulfillment().hashCode();
      result = 31 * result + Arrays.hashCode(getData());
      return result;
    }

    @Override
    public String toString() {
      return "InterledgerFulfillPacket{"
          + "  fulfillment=" + getFulfillment().toString()
          + ",  data=" + Arrays.toString(getData())
          + '}';
    }
  }

}
