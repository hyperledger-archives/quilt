package org.interledger.link.http;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import com.google.common.base.Splitter;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Generates urls using multilateral mode (like the JS ilp-http-plugin https://github.com/interledgerjs/ilp-plugin-http)
 * In multilateral mode, if the outgoing url has a "%" placeholder, the placeholder will be replaced by the ILP address
 * segment immediately after the link address prefix. For example, given an outgoing url like
 * https://node%.operator.com/ilp with a destination address g.operator.spsp.0.fffff will be mapped to
 * https://node0.operator.com/ilp
 */
public class MultilateralUrlFactory implements OutgoingUrlFactory {

  private static Splitter DOT_SPLITTER = Splitter.on(".");

  private final Supplier<InterledgerAddress> operatorAddressSupplier;
  private final HttpUrl outgoingUrl;


  public MultilateralUrlFactory(Supplier<InterledgerAddress> operatorAddressSupplier,
                                HttpUrl outgoingUrl) {
    this.operatorAddressSupplier = operatorAddressSupplier;
    this.outgoingUrl = outgoingUrl;
  }

  @Override
  public HttpUrl getOutgoingUrl(InterledgerAddress destinationAddress) {
    InterledgerAddressPrefix operatorAddressPrefix = InterledgerAddressPrefix.from(operatorAddressSupplier.get());
    List<String> remainingSegments = remainingSegments(destinationAddress, operatorAddressPrefix);
    if (remainingSegments.size() > 1) {
      return HttpUrl.parse(outgoingUrl.toString().replace("%", remainingSegments.get(1)));
    } else {
      return outgoingUrl;
    }
  }

  private static List<String> remainingSegments(InterledgerAddress address, InterledgerAddressPrefix prefix) {
    if (address.startsWith(prefix)) {
      String addressMinusPrefix = address.getValue().replace(prefix.getValue(), "");
      return DOT_SPLITTER.splitToList(addressMinusPrefix)
          .stream()
          .filter(value -> !value.isEmpty())
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

}
