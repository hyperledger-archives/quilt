package org.interledger.quilt.jackson.link;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.interledger.link.LinkId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link LinkIdModule}.
 */
public class LinkModuleTest {

  private static final LinkId LINK_ID = LinkId.of(UUID.randomUUID().toString());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void shouldSerializeAndDeserialize() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new LinkIdModule());

    final LinkIdContainer expectedContainer = LinkIdContainer.builder()
        .linkId(LINK_ID)
        .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);
    final LinkIdContainer decodedJson = objectMapper.readValue(actualJson, LinkIdContainer.class);

    assertThat(decodedJson).isEqualTo(expectedContainer);
  }

  @Test
  public void shouldNotDeserialize() throws IOException {
    ObjectMapper objectMapperWithoutModule = new ObjectMapper(); // No Module!

    final LinkIdContainer expectedContainer = LinkIdContainer.builder()
        .linkId(LINK_ID)
        .build();

    expectedException.expect(MismatchedInputException.class);
    final String json = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    objectMapperWithoutModule.readValue(json, LinkIdContainer.class);
  }

}
