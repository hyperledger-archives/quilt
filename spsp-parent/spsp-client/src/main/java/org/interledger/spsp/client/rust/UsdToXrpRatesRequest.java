package org.interledger.spsp.client.rust;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

@Immutable
@JsonDeserialize(as = ImmutableUsdToXrpRatesRequest.class)
@JsonSerialize(as = ImmutableUsdToXrpRatesRequest.class)
public interface UsdToXrpRatesRequest {

  static ImmutableUsdToXrpRatesRequest.Builder builder() {
    return ImmutableUsdToXrpRatesRequest.builder();
  }

  @JsonProperty("USD")
  double usd();

  @JsonProperty("XRP")
  double xrp();
}
