package org.interledger.quilt.jackson.link;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.interledger.link.LinkType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link LinkTypeSerializer}.
 */
public class LinkTypeSerializerTest {

  private LinkType LINK_TYPE = LinkType.of(UUID.randomUUID().toString());

  private ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new LinkTypeModule());

  @Test
  public void shouldSerialize() throws IOException {
    final String actual = objectMapper.writeValueAsString(LINK_TYPE);
    assertThat(actual).isEqualTo("\"" + LINK_TYPE.value() + "\"");
  }

  @Test
  public void shouldSerializeInContainer() throws IOException {
    final LinkTypeContainer expectedContainer = LinkTypeContainer.builder()
        .linkType(LINK_TYPE)
        .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);

    assertThat(actualJson).isEqualTo("{\"link_type\":\"" + LINK_TYPE.value() + "\"}");
  }
}
