package org.interledger.ildcp;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.ildcp.asn.framework.IldcpCodecContextFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class IldcpUtils {

  /**
   * Converts an {@link IldcpResponse} to a corresponding ILP Fulfillment packet.
   *
   * @param ildcpResponse A {@link IldcpResponse} to encode and package into the `data` property of a new Prepare
   *                      packet.
   *
   * @return A {@link InterledgerPreparePacket} that conforms to the IL-DCP RFC.
   */
  public static InterledgerFulfillPacket fromIldcpResponse(final IldcpResponse ildcpResponse) {
    Objects.requireNonNull(ildcpResponse);

    // Convert IldcpResponse to bytes...
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      IldcpCodecContextFactory.oer().write(ildcpResponse, os);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    return InterledgerFulfillPacket.builder()
        .fulfillment(IldcpResponse.EXECUTION_FULFILLMENT)
        .data(os.toByteArray())
        .build();
  }

  /**
   * Converts an {@link IldcpResponse} to a corresponding ILP Fulfillment packet.
   *
   * @param packet A {@link InterledgerPreparePacket} to decode into an  IL-DCP response.
   *
   * @return A {@link IldcpResponse} that conforms to the IL-DCP RFC.
   */
  public static IldcpResponse toIldcpResponse(final InterledgerFulfillPacket packet) {
    Objects.requireNonNull(packet);

    // Convert IldcpResponse to bytes...
    try {
      final ByteArrayInputStream is = new ByteArrayInputStream(packet.getData());
      return IldcpCodecContextFactory.oer().read(IldcpResponse.class, is);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

}
