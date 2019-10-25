package org.interledger.quilt.jackson.link;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.interledger.link.LinkId;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link LinkIdDeserializer}.
 */
public class LinkIdDeserializerTest {

  private LinkId LINK_ID = LinkId.of(UUID.randomUUID().toString());

  private ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new LinkIdModule());

  @Test
  public void shouldDeserialize() throws IOException {
    final LinkId actual = objectMapper
        .readValue("\"" + LINK_ID.value() + "\"", LinkId.class);

    assertThat(actual).isEqualTo(LINK_ID);
  }

  @Test
  public void shouldDeserializeInContainer() throws IOException {
    final LinkIdContainer expectedContainer = LinkIdContainer.builder()
        .linkId(LINK_ID)
        .build();

    final LinkIdContainer actualContainer = objectMapper.readValue(
        "{\"link_id\":\"" + LINK_ID.value() + "\"}",
        LinkIdContainer.class
    );

    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

}
