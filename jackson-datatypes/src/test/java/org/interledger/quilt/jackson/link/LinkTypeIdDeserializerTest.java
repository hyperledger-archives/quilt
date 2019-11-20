package org.interledger.quilt.jackson.link;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.interledger.link.LinkType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link LinkTypeDeserializer}.
 */
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:MemberName"})
public class LinkTypeIdDeserializerTest {
  private LinkType LINK_TYPE = LinkType.of(UUID.randomUUID().toString());

  private ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new LinkTypeModule());

  @Test
  public void shouldDeserialize() throws IOException {
    final LinkType actual = objectMapper
        .readValue("\"" + LINK_TYPE.value() + "\"", LinkType.class);

    assertThat(actual).isEqualTo(LINK_TYPE);
  }

  @Test
  public void shouldDeserializeInContainer() throws IOException {
    final LinkTypeContainer expectedContainer = LinkTypeContainer.builder()
        .linkType(LINK_TYPE)
        .build();

    final LinkTypeContainer actualContainer = objectMapper.readValue(
        "{\"link_type\":\"" + LINK_TYPE.value() + "\"}",
        LinkTypeContainer.class
    );

    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

}
