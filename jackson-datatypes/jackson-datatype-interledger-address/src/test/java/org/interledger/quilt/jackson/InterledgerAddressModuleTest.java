package org.interledger.quilt.jackson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.InterledgerAddress;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

/**
 * Validates the functionality of {@link InterledgerAddressModule}.
 */
public class InterledgerAddressModuleTest {

  private static final String HEX_CONDITION_DER_BYTES =
      "A02580202BB80D537B1DA3E38BD30361AA855686BDE0EACD7162FEF6A25FE97BF527A25B810106";

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new InterledgerAddressModule());
  }

  @Test
  public void testSerializeDeserialize() throws IOException {

    final InterledgerAddress expectedAddress = InterledgerAddress.of("test1.ledger.foo.");

    final InterledgerContainer expectedContainer = new InterledgerContainer(expectedAddress);

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"ledger_prefix\":\"%s\"}",
            expectedContainer.getInterledgerAddress().getValue(),
            HEX_CONDITION_DER_BYTES)
    ));

    final InterledgerContainer actualAddressContainer = objectMapper
        .readValue(json, InterledgerContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getInterledgerAddress(), is(expectedAddress));
  }


  private static class InterledgerContainer {

    @JsonProperty("ledger_prefix")
    private final InterledgerAddress interledgerAddress;

    @JsonCreator
    public InterledgerContainer(
        @JsonProperty("ledger_prefix") final InterledgerAddress interledgerAddress
    ) {
      this.interledgerAddress = Objects.requireNonNull(interledgerAddress);
    }

    public InterledgerAddress getInterledgerAddress() {
      return interledgerAddress;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InterledgerContainer that = (InterledgerContainer) o;
      return Objects.equals(getInterledgerAddress(), that.getInterledgerAddress());
    }

    @Override
    public int hashCode() {

      return Objects.hash(getInterledgerAddress());
    }

    @Override
    public String toString() {
      return "InterledgerContainer{" +
          "interledgerAddress=" + interledgerAddress +
          '}';
    }
  }
}