package org.interledger.node.services.fx;

import javax.money.convert.ExchangeRate;
import java.time.Instant;

public interface RateConverter extends ExchangeRate {

  ConversionResult convert(long sourceAmount, Instant sourceExpiry);

}
