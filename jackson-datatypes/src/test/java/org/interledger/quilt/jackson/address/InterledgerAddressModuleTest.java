package org.interledger.quilt.jackson.address;

/*-
 * ========================LICENSE_START=================================
 * Interledger Jackson Datatypes
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

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
    final InterledgerAddress expectedAddress = InterledgerAddress.of("test1.ledger.foo");

    final InterledgerContainer expectedContainer = new InterledgerContainer(expectedAddress);

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json).isEqualTo(
        String.format("{\"ledger_prefix\":\"%s\"}",
            expectedContainer.getInterledgerAddress().getValue(),
            HEX_CONDITION_DER_BYTES)
    );

    final InterledgerContainer actualAddressContainer = objectMapper
        .readValue(json, InterledgerContainer.class);

    assertThat(actualAddressContainer).isEqualTo(expectedContainer);
    assertThat(actualAddressContainer.getInterledgerAddress()).isEqualTo(expectedAddress);
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
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      InterledgerContainer that = (InterledgerContainer) obj;
      return Objects.equals(getInterledgerAddress(), that.getInterledgerAddress());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getInterledgerAddress());
    }

    @Override
    public String toString() {
      return "InterledgerContainer{"
          + "interledgerAddress=" + interledgerAddress
          + '}';
    }
  }
}
