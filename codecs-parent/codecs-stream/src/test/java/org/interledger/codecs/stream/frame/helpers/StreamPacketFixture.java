package org.interledger.codecs.stream.frame.helpers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

import java.util.List;

/**
 * A data model object that corresponds to the JSON object defined in `StreamPacketFixtures.json`.
 */
@Immutable
@JsonDeserialize(as = ImmutableStreamPacketFixture.class)
@JsonSerialize(as = ImmutableStreamPacketFixture.class)
public interface StreamPacketFixture {

  String sequence();

  short packetType();

  String amount();

  List<StreamFrameFixture> frames();

}
