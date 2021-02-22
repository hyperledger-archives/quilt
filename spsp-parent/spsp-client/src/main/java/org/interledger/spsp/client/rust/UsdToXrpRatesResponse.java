package org.interledger.spsp.client.rust;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

@Immutable
@JsonDeserialize(as = ImmutableUsdToXrpRatesResponse.class)
@JsonSerialize(as = ImmutableUsdToXrpRatesResponse.class)
public interface UsdToXrpRatesResponse {

  static ImmutableUsdToXrpRatesResponse.Builder builder() {
    return ImmutableUsdToXrpRatesResponse.builder();
  }

  @JsonProperty("USD")
  double usd();

  @JsonProperty("XRP")
  double xrp();
}
