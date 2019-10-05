package org.interledger.spsp.client.rust;

import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.net.URI;
import java.util.Optional;

/**
 * Account object for Interledger-RS HTTP API.
 *
 * @see "https://github.com/interledger-rs/interledger-rs/blob/master/docs/api.md"
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAccount.class)
@JsonDeserialize(builder = ImmutableAccount.Builder.class)
public interface Account {

  static ImmutableAccount.Builder builder() {
    return ImmutableAccount.builder();
  }

  /**
   * ILP address.
   *
   * @return address
   */
  @JsonProperty("ilp_address")
  InterledgerAddress ilpAddress();

  /**
   * Name of the account.
   *
   * @return name
   */
  String username();

  /**
   * Asset code for this account.
   *
   * @return code
   */
  @JsonProperty("asset_code")
  String assetCode();

  /**
   * Scale to use for amounts of the asset code (for example, a scale of 9 means that an amount of 1000000000 in an ILP
   * packet sent between peers represents 1 unit of that asset).
   *
   * @return An int representing the asset scale.
   */
  @JsonProperty("asset_scale")
  int assetScale();

  /**
   * Maximum amount that can transferred in a packet.
   *
   * @return amount or empty if not set
   */
  @JsonProperty("max_packet_amount")
  Optional<BigInteger> maxPacketAmount();

  /**
   * Mininum balance to be maintained on this account.
   *
   * @return amount or empty if not set
   */
  @JsonProperty("min_balance")
  Optional<BigInteger> minBalance();

  /**
   * http bearer token the client will use to authenticate with ILP node.
   *
   * @return token or emmpty for send only accounts
   */
  @JsonProperty("http_incoming_token")
  Optional<String> httpIncomingToken();

  /**
   * http bearer token ILP node will use to authenticate to client.
   *
   * @return token or empty for receive only accounts
   */
  @JsonProperty("http_outgoing_token")
  Optional<String> httpOutgoingToken();

  /**
   * URI for HTTP.
   *
   * @return http uri or empty if not set
   */
  @JsonProperty("http_endpoint")
  Optional<URI> httpEndpoint();

  /**
   * URI for BTP.
   *
   * @return btp uri or empty if not set
   */
  @JsonProperty("btp_uri")
  Optional<URI> btpUri();

  /**
   * Threshold amount that triggers settlement.
   *
   * @return amount or empty if not set
   */
  @JsonProperty("settle_threshold")
  Optional<BigInteger> settleThreshold();

  /**
   * When settlement occurs, that balance that should be settled to.
   *
   * @return amount or empty if not set
   */
  @JsonProperty("settle_to")
  Optional<BigInteger> settleTo();

  /**
   * Type of routing relation.
   *
   * @return relation or empty if not set
   */
  @JsonProperty("routing_relation")
  Optional<RoutingRelation> routingRelation();

  /**
   * round trip time in milliseconds.
   *
   * @return time or empty if not set
   */
  @JsonProperty("round_trip_time")
  Optional<BigInteger> roundTripTime();

  /**
   * Amount of asset that can be sent per minute.
   *
   * @return amount or empty if not set
   */
  @JsonProperty("amount_per_minute_limit")
  Optional<BigInteger> amountPerMinuteLimit();

  /**
   * Amount of packets that can be sent per minute.
   *
   * @return amount or empty if not set
   */
  @JsonProperty("packets_per_minute_limit")
  Optional<BigInteger> packetsPerMinuteLimit();

  /**
   * Types of routing relationships for an account.
   */
  enum RoutingRelation {
    PEER("Peer"),
    PARENT("Parent"),
    CHILD("Child"),
    NON_ROUTING_ACCOUNT("NonRoutingAccount");

    private final String code;

    RoutingRelation(String code) {
      this.code = code;
    }

    @JsonValue
    public String getCode() {
      return code;
    }
  }

}
