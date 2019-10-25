package org.interledger.quilt.jackson.link;

import org.interledger.link.LinkType;
import org.interledger.quilt.jackson.link.ImmutableLinkTypeContainer.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableLinkTypeContainer.class)
@JsonDeserialize(as = ImmutableLinkTypeContainer.class)
public interface LinkTypeContainer {

  static Builder builder() {
    return ImmutableLinkTypeContainer.builder();
  }

  @JsonProperty("link_type")
  LinkType getLinkType();
}
