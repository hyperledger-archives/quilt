package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
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
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error.ILLEGAL_ENDING;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error.INVALID_SCHEME_PREFIX;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error.SEGMENTS_UNDERFLOW;

import org.interledger.core.InterledgerAddress.AllocationScheme;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

/**
 * Unit tests for {@link InterledgerAddress.AllocationScheme}.
 */
@RunWith(Parameterized.class)
public class InterledgerAddressSchemeTest {

  private final String allocationScheme;

  public InterledgerAddressSchemeTest(final String allocationScheme) {
    this.allocationScheme = allocationScheme;
  }

  /**
   * Generates an {@link Iterable} of arrays containing Strings that will be passed to each of the
   * test methods of this test.
   */
  @Parameters(name = "{index}: allocationScheme({0})")
  public static Iterable<Object[]> schemes() {
    return Arrays.asList(new Object[][] {
        {"g"}, {"private"}, {"example"}, {"peer"}, {"self"}, {"test1"}, {"test2"}, {"test3"}
    });
  }

  /**
   * Assert that each scheme is validly created.
   */
  @Test
  public void test_allocation_scheme_() {
    final AllocationScheme allocationScheme = InterledgerAddress.AllocationScheme
        .builder()
        .value(this.allocationScheme)
        .build();
    assertThat(allocationScheme.getValue()).isEqualTo(this.allocationScheme);
  }

  /**
   * Assert that something like "g.foo" is invalid.
   */
  @Test(expected = IllegalArgumentException.class)
  public void test_allocation_scheme__with_too_much() {
    try {
      AllocationScheme.builder().value(this.allocationScheme + ".foo").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SCHEME_PREFIX.getMessageFormat(),
          this.allocationScheme + "" + ".foo"));
      throw e;
    }
  }

  /**
   * Assert that something like "g.foo." is invalid.
   */
  @Test(expected = IllegalArgumentException.class)
  public void test_allocation_scheme__with_too_much_plus_trailing_dot() {
    try {
      AllocationScheme.builder().value(this.allocationScheme + ".foo.").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SCHEME_PREFIX.getMessageFormat(),
          this.allocationScheme + ".foo."));
      throw e;
    }
  }

  /**
   * Assert that creating an address with something like "g." is invalid.
   */
  @Test(expected = IllegalArgumentException.class)
  public void test_allocation_scheme__with_trailing_dot() {
    try {
      AllocationScheme.builder().value(this.allocationScheme + ".").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SCHEME_PREFIX.getMessageFormat(),
          this.allocationScheme + "."));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_allocation_scheme_address_with_invalid_scheme() {
    try {
      AllocationScheme.builder().value(this.allocationScheme + "1.foo").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SCHEME_PREFIX.getMessageFormat(),
          this.allocationScheme + "1.foo"));
      throw e;
    }
  }

  /////////////////////////////
  // Address tests with schemes
  /////////////////////////////

  /**
   * Assert that something like "g.foo.bob" is valid.
   */
  @Test
  public void test_scheme_with_neighborhood_and_account_as_address() {
    final InterledgerAddress address =
        InterledgerAddress.builder().value(this.allocationScheme + ".foo.bob").build();
    assertThat(address.getValue()).isEqualTo(this.allocationScheme + ".foo.bob");
  }

  /**
   * Assert that something like "g.foo.bar" is valid.
   */
  @Test
  public void test_scheme_with_only_address() {
    final InterledgerAddress address =
        InterledgerAddress.builder().value(this.allocationScheme + ".foo.bar").build();
    assertThat(address.getValue()).isEqualTo(this.allocationScheme + ".foo.bar");
  }

  /**
   * Assert that something like "g.foo" is valid.
   */
  @Test
  public void test_scheme_with_neighborhood_as_prefix() {
    final InterledgerAddress addressPrefix =
        InterledgerAddress.builder().value(this.allocationScheme + ".foo").build();
    assertThat(addressPrefix.getValue()).isEqualTo(this.allocationScheme + ".foo");
  }

  /**
   * Assert that creating an address with something like "g." is invalid.
   */
  @Test(expected = IllegalArgumentException.class)
  public void test_address_with_only_scheme_and_dot() {
    try {
      InterledgerAddress.builder().value(this.allocationScheme + ".").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(ILLEGAL_ENDING.getMessageFormat());
      throw e;
    }
  }

  /**
   * Assert that creating an address with something like "g" is invalid.
   */
  @Test(expected = IllegalArgumentException.class)
  public void test_address_with_only_scheme() {
    try {
      InterledgerAddress.builder().value(this.allocationScheme).build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(SEGMENTS_UNDERFLOW.getMessageFormat());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_address_with_invalid_scheme() {
    try {
      InterledgerAddress.builder().value(this.allocationScheme + "1.foo").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SCHEME_PREFIX.getMessageFormat(),
          this.allocationScheme + "1"));
      throw e;
    }
  }
}
