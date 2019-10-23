package org.interledger.quilt.jackson.link;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.interledger.link.LinkId;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link LinkIdSerializer}.
 */
public class LinkIdSerializerTest {

  private LinkId LINK_ID = LinkId.of(UUID.randomUUID().toString());

  private ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new LinkIdModule());

  @Test
  public void shouldSerialize() throws IOException {
    final String actual = objectMapper.writeValueAsString(LINK_ID);
    assertThat(actual).isEqualTo("\"" + LINK_ID.value() + "\"");
  }

  @Test
  public void shouldSerializeInContainer() throws IOException {
    final LinkIdContainer expectedContainer = LinkIdContainer.builder()
        .linkId(LINK_ID)
        .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);

    assertThat(actualJson).isEqualTo("{\"link_id\":\"" + LINK_ID.value() + "\"}");
  }
}
