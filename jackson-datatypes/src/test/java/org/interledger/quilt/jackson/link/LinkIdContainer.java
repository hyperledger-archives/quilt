package org.interledger.quilt.jackson.link;

import org.interledger.link.LinkId;
import org.interledger.quilt.jackson.link.ImmutableLinkIdContainer.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableLinkIdContainer.class)
@JsonDeserialize(as = ImmutableLinkIdContainer.class)
public interface LinkIdContainer {

  static Builder builder() {
    return ImmutableLinkIdContainer.builder();
  }

  @JsonProperty("link_id")
  LinkId getLinkId();
}
