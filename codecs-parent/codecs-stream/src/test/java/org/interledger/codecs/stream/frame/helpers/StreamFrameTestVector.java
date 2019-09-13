package org.interledger.codecs.stream.frame.helpers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

import java.util.Optional;

@Immutable
@JsonDeserialize(as = ImmutableStreamFrameTestVector.class)
@JsonSerialize(as = ImmutableStreamFrameTestVector.class)
public interface StreamFrameTestVector {

  String type();

  Optional<Short> errorCode();

  Optional<String> errorMessage();

  Optional<String> sourceAddress();

  Optional<Long> streamId();

  Optional<Long> maxStreamId();

  Optional<Short> offset();

  Optional<Short> maxOffset();

  Optional<Long> shares();

  Optional<Long> receiveMax();

  Optional<Long> totalReceived();

  Optional<Long> sendMax();

  Optional<Long> totalSent();

  Optional<String> sourceAssetCode();

  Optional<Short> sourceAssetScale();

  Optional<String> base64Data();

  String expectedAsn1OerBytes();

}
