package org.interledger.quilt.jackson.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.quilt.jackson.stream.StreamSharedSecretModuleTest.StreamSharedSecretContainer;
import org.interledger.stream.crypto.StreamSharedSecret;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Unit tests for {@link StreamSharedSecretDeserializer}.
 */
public class StreamSharedSecretDeserializerTest {

  private static final StreamSharedSecret SHARED_SECRET = StreamSharedSecret.of(new byte[32]);

  protected ObjectMapper objectMapper;

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
      .registerModule(new StreamSharedSecretModule());
  }

  @Test
  public void shouldDeserialize() throws IOException {
    final StreamSharedSecret actual = objectMapper
      .readValue("\"" + SHARED_SECRET.value() + "\"", StreamSharedSecret.class);

    assertThat(actual).isEqualTo(SHARED_SECRET);
  }

  @Test
  public void shouldDeserializeInContainer() throws IOException {
    final StreamSharedSecretContainer expectedContainer = ImmutableStreamSharedSecretContainer.builder()
      .streamSharedSecret(SHARED_SECRET)
      .build();

    final StreamSharedSecretContainer actualContainer = objectMapper.readValue(
      "{\"shared_secret\":\"" + SHARED_SECRET.value() + "\"}",
      StreamSharedSecretContainer.class
    );

    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

}
