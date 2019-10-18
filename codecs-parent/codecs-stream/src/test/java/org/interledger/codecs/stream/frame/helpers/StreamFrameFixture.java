package org.interledger.codecs.stream.frame.helpers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

import java.util.Optional;

/**
 * A data model object that corresponds to the JSON object defined in `StreamPacketFixtures.json`.
 */
@Immutable
@JsonDeserialize(as = ImmutableStreamFrameFixture.class)
@JsonSerialize(as = ImmutableStreamFrameFixture.class)
public interface StreamFrameFixture {

  short type();

  String name();

  Optional<Short> errorCode();

  Optional<String> errorMessage();

  Optional<String> sourceAccount();

  Optional<String> streamId();

  Optional<String> maxStreamId();

  Optional<String> offset();

  Optional<String> maxOffset();

  Optional<String> shares();

  Optional<String> receiveMax();

  Optional<String> totalReceived();

  Optional<String> sendMax();

  Optional<String> totalSent();

  Optional<String> sourceAssetCode();

  Optional<String> sourceAssetScale();

  Optional<String> data();
}
