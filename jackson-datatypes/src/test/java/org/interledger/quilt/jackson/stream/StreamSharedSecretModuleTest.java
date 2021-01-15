package org.interledger.quilt.jackson.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.stream.crypto.StreamSharedSecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.immutables.value.Value;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

/**
 * Unit tests for {@link StreamSharedSecretModule}.
 */
public class StreamSharedSecretModuleTest {

  private static final StreamSharedSecret SHARED_SECRET = StreamSharedSecret.of(new byte[32]);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  protected ObjectMapper objectMapper;

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
      .registerModule(new StreamSharedSecretModule());
  }

  @Test
  public void shouldSerializeAndDeserialize() throws IOException {
    final StreamSharedSecretContainer expectedContainer = ImmutableStreamSharedSecretContainer.builder()
      .streamSharedSecret(SHARED_SECRET)
      .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    final StreamSharedSecretContainer actualContainer = objectMapper
      .readValue(json, StreamSharedSecretContainer.class);

    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

  @Test
  public void shouldNotSerializeAndDeserialize() throws IOException {
    ObjectMapper objectMapperWithoutModule = new ObjectMapper(); // No Module!
    final StreamSharedSecretContainer expectedContainer = ImmutableStreamSharedSecretContainer.builder()
      .streamSharedSecret(SHARED_SECRET)
      .build();

    expectedException.expect(InvalidDefinitionException.class);
    final String actualJson = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    objectMapperWithoutModule.readValue(actualJson, StreamSharedSecretContainer.class);
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableStreamSharedSecretContainer.class)
  @JsonDeserialize(as = ImmutableStreamSharedSecretContainer.class)
  public interface StreamSharedSecretContainer {

    @JsonProperty("shared_secret")
    StreamSharedSecret streamSharedSecret();
  }
}
