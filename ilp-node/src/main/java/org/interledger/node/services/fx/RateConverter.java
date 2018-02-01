package org.interledger.node.services.fx;

import java.time.Instant;

public interface RateConverter {

  ConversionResult convert(long sourceAmount, Instant sourceExpiry);

}
