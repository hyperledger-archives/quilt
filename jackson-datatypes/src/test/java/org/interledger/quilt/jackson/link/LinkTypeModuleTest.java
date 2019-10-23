package org.interledger.quilt.jackson.link;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.interledger.link.LinkType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link LinkTypeModule}.
 */
public class LinkTypeModuleTest {

  private static final LinkType LINK_TYPE = LinkType.of(UUID.randomUUID().toString());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void shouldSerializeAndDeserializeWithModule() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new LinkTypeModule());
    final LinkTypeContainer expectedContainer = LinkTypeContainer.builder()
        .linkType(LINK_TYPE)
        .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);
    final LinkTypeContainer decodedJson = objectMapper.readValue(actualJson, LinkTypeContainer.class);
    assertThat(decodedJson).isEqualTo(expectedContainer);
  }

  @Test
  public void shouldNotDeserialize() throws IOException {
    ObjectMapper objectMapperWithoutModule = new ObjectMapper();  // No Module!
    final LinkTypeContainer expectedContainer = LinkTypeContainer.builder()
        .linkType(LINK_TYPE)
        .build();

    expectedException.expect(MismatchedInputException.class);
    final String json = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    objectMapperWithoutModule.readValue(json, LinkTypeContainer.class);
  }

}
