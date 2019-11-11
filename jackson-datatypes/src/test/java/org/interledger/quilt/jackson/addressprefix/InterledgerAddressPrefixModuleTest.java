package org.interledger.quilt.jackson.addressprefix;

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

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

/**
 * Validates the functionality of {@link InterledgerAddressPrefixModule}.
 */
public class InterledgerAddressPrefixModuleTest {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new InterledgerAddressPrefixModule());
  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final InterledgerAddressPrefix expectedAddress = InterledgerAddressPrefix.of("test1.ledger.foo");

    final InterledgerContainer expectedContainer = new InterledgerContainer(expectedAddress);

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json).isEqualTo(
        String.format("{\"ledger_prefix\":\"%s\"}", expectedContainer.getInterledgerAddressPrefix().getValue())
    );

    final InterledgerContainer actualAddressContainer = objectMapper
        .readValue(json, InterledgerContainer.class);

    assertThat(actualAddressContainer).isEqualTo(expectedContainer);
    assertThat(actualAddressContainer.getInterledgerAddressPrefix()).isEqualTo(expectedAddress);
  }


  private static class InterledgerContainer {

    @JsonProperty("ledger_prefix")
    private final InterledgerAddressPrefix interledgerAddressPrefix;

    @JsonCreator
    public InterledgerContainer(
        @JsonProperty("ledger_prefix") final InterledgerAddressPrefix interledgerAddress
    ) {
      this.interledgerAddressPrefix = Objects.requireNonNull(interledgerAddress);
    }

    public InterledgerAddressPrefix getInterledgerAddressPrefix() {
      return interledgerAddressPrefix;
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
      return Objects.equals(getInterledgerAddressPrefix(), that.getInterledgerAddressPrefix());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getInterledgerAddressPrefix());
    }

    @Override
    public String toString() {
      return "InterledgerContainer{"
          + "interledgerAddressPrefix=" + interledgerAddressPrefix
          + '}';
    }
  }
}
