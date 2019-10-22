package org.interledger.codecs.stream.frame.helpers;

import org.interledger.codecs.stream.frame.helpers.ImmutableStreamTestFixture.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

import java.util.Optional;

/**
 * A data model object that corresponds to the JSON object defined in `StreamPacketFixtures.json`.
 */
@Immutable
@JsonDeserialize(as = ImmutableStreamTestFixture.class)
@JsonSerialize(as = ImmutableStreamTestFixture.class)
public interface StreamTestFixture {

  static Builder builder() {
    return ImmutableStreamTestFixture.builder();
  }

  String name();

  StreamPacketFixture packet();

  String buffer();

  @JsonProperty("decode_only")
  Optional<Boolean> decodeOnly();
}
