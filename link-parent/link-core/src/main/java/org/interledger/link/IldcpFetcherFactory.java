package org.interledger.link;

import java.util.Objects;
import org.interledger.codecs.ildcp.IldcpUtils;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpRequestPacket;

/**
 * Factory to create {@link IldcpFetcher}
 */
public interface IldcpFetcherFactory {

  /**
   * Create {@link IldcpFetcher} for a give link
   *
   * @param link
   * @return
   */
  IldcpFetcher construct(Link link);

  /**
   * Default implementation that performs IL-DCP by sending a {@link IldcpRequestPacket} over the provided {@link
   * Link}.
   */
  class Default implements IldcpFetcherFactory {

    @Override
    public IldcpFetcher construct(final Link link) {
      Objects.requireNonNull(link);

      return ildcpRequest -> {
        Objects.requireNonNull(ildcpRequest);

        final IldcpRequestPacket ildcpRequestPacket = IldcpRequestPacket.builder().build();
        final InterledgerPreparePacket preparePacket =
          InterledgerPreparePacket.builder().from(ildcpRequestPacket).build();

        // Fetch the IL-DCP response using the Link.
        return link.sendPacket(preparePacket)
          .map(
            // If FulfillPacket...
            IldcpUtils::toIldcpResponse,
            // If Reject Packet...
            (interledgerRejectPacket) -> {
              throw new RuntimeException(
                String.format("IL-DCP negotiation failed! interledgerRejectPacket=%s", interledgerRejectPacket)
              );
            }
          );
      };
    }

  }
}
