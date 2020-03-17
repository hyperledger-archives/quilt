package org.interledger.quilt.jackson.spsp;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.SharedSecret;
import org.interledger.quilt.jackson.sharedsecret.SharedSecretModule;

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
 * Unit tests for {@link SharedSecretModule}.
 */
public class SharedSecretModuleTest {

  private static final SharedSecret SHARED_SECRET = SharedSecret.of(new byte[32]);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  protected ObjectMapper objectMapper;

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new SharedSecretModule());
  }

  @Test
  public void shouldSerializeAndDeserialize() throws IOException {
    final SharedSecretContainer expectedContainer = ImmutableSharedSecretContainer.builder()
        .sharedSecret(SHARED_SECRET)
        .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    final SharedSecretContainer actualContainer = objectMapper
        .readValue(json, SharedSecretContainer.class);

    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

  @Test
  public void shouldNotSerializeAndDeserialize() throws IOException {
    ObjectMapper objectMapperWithoutModule = new ObjectMapper(); // No Module!
    final SharedSecretContainer expectedContainer = ImmutableSharedSecretContainer.builder()
        .sharedSecret(SHARED_SECRET)
        .build();

    expectedException.expect(InvalidDefinitionException.class);
    final String actualJson = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    objectMapperWithoutModule.readValue(actualJson, SharedSecretContainer.class);
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableSharedSecretContainer.class)
  @JsonDeserialize(as = ImmutableSharedSecretContainer.class)
  public interface SharedSecretContainer {

    @JsonProperty("shared_secret")
    SharedSecret getSharedSecret();
  }
}
