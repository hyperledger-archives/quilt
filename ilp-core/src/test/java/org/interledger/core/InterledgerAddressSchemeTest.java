package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Interledger
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.core.InterledgerAddress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

/**
 * Unit tests for {@link ImmutableInterledgerAddress.Builder} schemes.
 */
@RunWith(Parameterized.class)
public class InterledgerAddressSchemeTest {

  private static final String EXPECTED_ERROR_MESSAGE = "Address is invalid";
  private final String scheme;

  public InterledgerAddressSchemeTest(final String scheme) {
    this.scheme = scheme;
  }

  /**
   * Generates an {@link Iterable} of arrays containing Strings that will be passed to each of the
   * test methods of this test.
   */
  @Parameters(name = "{index}: scheme({0})")
  public static Iterable<Object[]> schemes() {
    return Arrays.asList(new Object[][]{{"g"}, {"private"}, {"example"}, {"peer"}, {"self"},
        {"test1"}, {"test2"}, {"test3"}});
  }

  /**
   * Assert that something like "g.foo.bob" is valid.
   */
  @Test
  public void test_scheme_with_neighborhood_and_account_as_address() throws Exception {
    final InterledgerAddress address =
        InterledgerAddress.builder().value(this.scheme + ".foo.bob").build();
    assertThat(address.getValue(), is(this.scheme + ".foo.bob"));
    assertThat(address.isLedgerPrefix(), is(false));
  }

  /**
   * Assert that something like "g.foo.bob." is valid.
   */
  @Test
  public void test_scheme_with_neighborhood_and_ledger_identifier_as_prefix() throws Exception {
    final InterledgerAddress addressPrefix =
        InterledgerAddress.builder().value(this.scheme + ".foo.bob.").build();
    assertThat(addressPrefix.getValue(), is(this.scheme + ".foo.bob."));
    assertThat(addressPrefix.isLedgerPrefix(), is(true));
  }

  /**
   * Assert that something like "g.foo" is valid.
   */
  @Test
  public void test_scheme_with_only_address() throws Exception {
    final InterledgerAddress address =
        InterledgerAddress.builder().value(this.scheme + ".foo.bar").build();
    assertThat(address.getValue(), is(this.scheme + ".foo.bar"));
    assertThat(address.isLedgerPrefix(), is(false));
  }

  /**
   * Assert that something like "g.foo." is valid.
   */
  @Test
  public void test_scheme_with_neighborhood_as_prefix() throws Exception {
    final InterledgerAddress addressPrefix =
        InterledgerAddress.builder().value(this.scheme + ".foo.").build();
    assertThat(addressPrefix.getValue(), is(this.scheme + ".foo."));
    assertThat(addressPrefix.isLedgerPrefix(), is(true));
  }

  /**
   * Assert that something like "g." is valid.
   */
  @Test
  public void test_address_with_only_scheme_prefix() throws Exception {
    final InterledgerAddress address =
        InterledgerAddress.builder().value(this.scheme + ".").build();
    assertThat(address.getValue(), is(this.scheme + "."));
    assertThat(address.isLedgerPrefix(), is(true));
  }

  /**
   * Assert that something like "g" is invalid.
   */
  @Test(expected = IllegalArgumentException.class)
  public void test_address_with_only_scheme_address() throws Exception {
    try {
      InterledgerAddress.builder().value(this.scheme).build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(EXPECTED_ERROR_MESSAGE));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_destination_address_with_invalid_scheme() throws Exception {
    try {
      InterledgerAddress.builder().value(this.scheme + "1.foo").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(EXPECTED_ERROR_MESSAGE));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_prefix_with_invalid_scheme() throws Exception {
    try {
      InterledgerAddress.builder().value(this.scheme + "1.foo.").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(EXPECTED_ERROR_MESSAGE));
      throw e;
    }
  }
}
