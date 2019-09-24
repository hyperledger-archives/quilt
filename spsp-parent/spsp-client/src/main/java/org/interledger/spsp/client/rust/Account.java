package org.interledger.spsp.client.rust;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Account object for Interledger-RS HTTP API.
 * {@see https://github.com/interledger-rs/interledger-rs/blob/master/docs/api.md}
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAccount.class)
@JsonDeserialize(builder = ImmutableAccount.Builder.class)
public interface Account {

  static ImmutableAccount.Builder builder() {
    return ImmutableAccount.builder();
  }

  @JsonProperty("ilp_address")
  InterledgerAddress ilpAddress();

  String username();

  @JsonProperty("asset_code")
  String assetCode();

  @JsonProperty("asset_scale")
  int assetScale();

  @JsonProperty("max_packet_amount")
  Optional<BigInteger> maxPacketAmount();

  @JsonProperty("min_balance")
  Optional<BigInteger> minBalance();

  /**
   * http bearer token the client will use to authenticate with ILP node
   * @return value
   */
  @JsonProperty("http_incoming_token")
  Optional<String> httpIncomingToken();

  @JsonProperty("http_endpoint")
  Optional<String> httpEndpoint();

  /**
   * http bearer token ILP node will use to authenticate to client
   * @return
   */
  @JsonProperty("http_outgoing_token")
  Optional<String> httpOutgoingToken();

  @JsonProperty("btp_uri")
  Optional<String> btpUri();

  @JsonProperty("settle_threshold")
  Optional<BigInteger> settleThreshold();

  @JsonProperty("settle_to")
  Optional<BigInteger> settleTo();

  @JsonProperty("routing_relation")
  Optional<String> routingRelation();

  @JsonProperty("round_trip_time")
  Optional<BigInteger> roundTripTime();

  @JsonProperty("amount_per_minute_limit")
  Optional<BigInteger> amountPerMinuteLimit();

  @JsonProperty("packets_per_minute_limit")
  Optional<BigInteger> packetsPerMinuteLimit();

}
