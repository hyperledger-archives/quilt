package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
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
import static org.interledger.core.InterledgerAddressPrefix.AbstractInterledgerAddressPrefix.Error.ADDRESS_OVERFLOW;
import static org.interledger.core.InterledgerAddressPrefix.AbstractInterledgerAddressPrefix.Error.ILLEGAL_ENDING;
import static org.interledger.core.InterledgerAddressPrefix.AbstractInterledgerAddressPrefix.Error.INVALID_SCHEME_PREFIX;
import static org.interledger.core.InterledgerAddressPrefix.AbstractInterledgerAddressPrefix.Error.INVALID_SEGMENT;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress.AllocationScheme;

import org.junit.Test;

/**
 * Unit tests for {@link InterledgerAddressPrefix}.
 */
public class InterledgerAddressPrefixTest {

  private static final String TEST1_US_USD = "test1.us.usd";
  private static final String TEST1_US_USD_BOB = TEST1_US_USD + ".bob";

  private static final String JUST_RIGHT =
      "g.foo.0123"
          + "45678901234567890123456789012345678901234567890123456789012345678901234567890123456"
          + "78901234567890123456789012345678901234567890123456789012345678901234567890123456789"
          + "01234567890123456789012345678901234567890123456789012345678901234567890123456789012"
          + "34567890123456789012345678901234567890123456789012345678901234567890123456789012345"
          + "67890123456789012345678901234567890123456789012345678901234567890123456789012345678"
          + "90123456789012345678901234567890123456789012345678901234567890123456789012345678901"
          + "23456789012345678901234567890123456789012345678901234567890123456789012345678901234"
          + "56789012345678901234567890123456789012345678901234567890123456789012345678901234567"
          + "89012345678901234567890123456789012345678901234567890123456789012345678901234567890"
          + "12345678901234567890123456789012345678901234567890123456789012345678901234567890123"
          + "45678901234567890123456789012345678901234567890123456789012345678901234567890123456"
          + "78901234567890123456789012345678901234567890123456789012345678901234567890123456789"
          + "01234567891234567";

  private static final String TOO_LONG = JUST_RIGHT + "0";
  private static final String G_FOO = "g.foo";

  ////////////////////////
  // Builder Tests
  ////////////////////////

  @Test(expected = IllegalStateException.class)
  public void test_constructor_with_uninitialized_build() {
    String errorMessage = "Cannot build InterledgerAddressPrefix, some of required attributes are not set [value]";
    try {
      InterledgerAddressPrefix.builder().build();
    } catch (IllegalStateException e) {
      //Removed message check. This exception is raised by the Immutable generated code.
      assertThat(e.getMessage()).isEqualTo(errorMessage);
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void test_wither_with_null_value() {
    try {
      InterledgerAddressPrefix.builder()
          .value(null)
          .build();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("value");
      throw e;
    }
  }

  @Test
  public void testConstruction_DeliverableAddress() {
    final InterledgerAddressPrefix address = InterledgerAddressPrefix.builder()
        .value(TEST1_US_USD_BOB).build();
    assertThat(address.getValue()).isEqualTo(TEST1_US_USD_BOB);
  }

  @Test
  public void testConstruction_LedgerPrefix() {
    final InterledgerAddressPrefix address = InterledgerAddressPrefix.builder().value(TEST1_US_USD)
        .build();
    assertThat(address.getValue()).isEqualTo(TEST1_US_USD);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_empty_address() {
    try {
      InterledgerAddressPrefix.builder().value("").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SCHEME_PREFIX.getMessageFormat(), ""));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_blank_address() {
    try {
      InterledgerAddressPrefix.builder().value("  ").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SCHEME_PREFIX.getMessageFormat(), "  "));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_address_with_space() {
    try {
      InterledgerAddressPrefix.builder().value(TEST1_US_USD_BOB + " space").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SEGMENT.getMessageFormat(), "bob space"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_address_too_long() {
    try {
      InterledgerAddressPrefix.builder().value(TOO_LONG).build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(ADDRESS_OVERFLOW.getMessageFormat());
      throw e;
    }
  }

  @Test
  public void test_address_just_right_length() {
    // This is 1023 characters long...
    final InterledgerAddressPrefix address = InterledgerAddressPrefix.builder().value(JUST_RIGHT)
        .build();
    assertThat(address.getValue()).isEqualTo(JUST_RIGHT);
  }

  @Test
  public void test_address_all_valid_characters() {
    final String allValues = "g.0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_~.-";
    final InterledgerAddressPrefix address = InterledgerAddressPrefix.builder().value(allValues)
        .build();
    assertThat(address.getValue()).isEqualTo(allValues);
  }

  ////////////////////////
  // Accessors
  ////////////////////////

  @Test
  public void testValue() {
    assertThat(InterledgerAddressPrefix.of("g.foo.bob").getValue()).isEqualTo("g.foo.bob");
  }

  @Test(expected = NullPointerException.class)
  public void testOfWithNull() {
    try {
      InterledgerAddressPrefix.of(null);
      fail("should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("value must not be null!");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOfWithEmptyString() {
    try {
      InterledgerAddressPrefix.of("");
      fail("should have thrown an exception but did not!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("The '' AllocationScheme is invalid!");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOfWithBlankString() {
    try {
      InterledgerAddressPrefix.of(" ");
      fail("should have thrown an exception but did not!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("The ' ' AllocationScheme is invalid!");
      throw e;
    }
  }

  @Test
  public void testStartsWithString() {
    final InterledgerAddressPrefix address = InterledgerAddressPrefix.of("g.foo.bob");
    assertThat(address.startsWith("g")).isTrue();
    assertThat(address.startsWith(G_FOO)).isTrue();
    assertThat(address.startsWith("g.foo.bob")).isTrue();
    assertThat(address.startsWith("test.foo.bob")).isFalse();
  }

  @Test
  public void testStartsWithInterledgerAddressPrefix() {
    final InterledgerAddressPrefix address = InterledgerAddressPrefix.of("g.foo.bob");
    assertThat(address.startsWith(InterledgerAddressPrefix.of(G_FOO))).isTrue();
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo.bob"))).isTrue();
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo.bob.bar"))).isFalse();
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo.bobbar"))).isFalse();
    assertThat(address.startsWith(InterledgerAddressPrefix.of("test1.foo.bob"))).isFalse();
  }

  @Test(expected = NullPointerException.class)
  public void testAddressWithNull() {
    final InterledgerAddressPrefix addressPrefix = InterledgerAddressPrefix.of(G_FOO);
    try {
      addressPrefix.with(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("addressSegment must not be null!");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressWithEmpty() {
    final InterledgerAddressPrefix addressPrefix = InterledgerAddressPrefix.of(G_FOO);
    try {
      addressPrefix.with("");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(ILLEGAL_ENDING.getMessageFormat());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressWithBlank() {
    final InterledgerAddressPrefix addressPrefix = InterledgerAddressPrefix.of(G_FOO);
    try {
      addressPrefix.with("  ");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SEGMENT.getMessageFormat(), "  "));
      throw e;
    }
  }

  /**
   * Validates adding an address prefix (as an InterledgerAddressPrefix) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithAddressPrefix() {
    final InterledgerAddressPrefix destinationAddress = InterledgerAddressPrefix.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue()).isEqualTo("g.foo.bob.boz");
  }

  /**
   * Validates adding a destination address (as an InterledgerAddressPrefix) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithDestinationAddress() {
    final InterledgerAddressPrefix addressPrefix = InterledgerAddressPrefix.of(G_FOO);
    final String additionalDestinationAddress = "bob";
    assertThat(addressPrefix.with(additionalDestinationAddress).getValue()).isEqualTo("g.foo.bob");
  }

  /**
   * Validates adding a destination address (as an InterledgerAddressPrefix) to a destination
   * address.
   */
  @Test
  public void testDestinationAddressWithDestinationAddress() {
    final InterledgerAddressPrefix destinationAddress = InterledgerAddressPrefix.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue()).isEqualTo("g.foo.bob.boz");
  }

  /**
   * Validates adding an address prefix  (as an InterledgerAddressPrefix) to a destination address.
   */
  @Test
  public void testAddressWithAddress() {
    final InterledgerAddressPrefix destinationAddress = InterledgerAddressPrefix.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue()).isEqualTo("g.foo.bob.boz");
  }

  @Test
  public void testAllocationScheme() {
    final InterledgerAddressPrefix prefix = InterledgerAddressPrefix.of("g");
    assertThat(prefix.getValue()).isEqualTo("g");
  }

  @Test
  public void testGetPrefixFromAllocationSchemePrefix() {
    assertThat(InterledgerAddressPrefix.of("g").getPrefix().isPresent()).isFalse();
  }

  @Test
  public void testGetPrefixFromShortPrefix() {
    assertThat(InterledgerAddressPrefix.of("g.1").getPrefix().isPresent()).isTrue();
  }

  @Test
  public void testGetPrefixFromPrefix() {
    final InterledgerAddressPrefix address = InterledgerAddressPrefix.of("g.example");
    assertThat(address.getPrefix().isPresent()).isTrue();
  }

  @Test
  public void testGetPrefixFromLongPrefix() {
    final InterledgerAddressPrefix address = InterledgerAddressPrefix
        .of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().get().getValue()).isEqualTo("g.alpha.beta.charlie.delta");
  }

  @Test
  public void testGetPrefixFromAddress() {
    final InterledgerAddressPrefix address = InterledgerAddressPrefix.of("g.example.bob");
    assertThat(address.getPrefix().get().getValue()).isEqualTo("g.example");
  }

  @Test
  public void testGetPrefixFromShortAddress() {
    assertThat(InterledgerAddressPrefix.of("g.bob.foo").getPrefix().get().getValue()).isEqualTo("g.bob");
    assertThat(InterledgerAddressPrefix.of("g.b.f").getPrefix().get().getValue()).isEqualTo("g.b");
  }

  @Test
  public void testGetPrefixFromLongAddress() {
    final InterledgerAddressPrefix address = InterledgerAddressPrefix
        .of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().get().getValue()).isEqualTo("g.alpha.beta.charlie.delta");
  }

  @Test
  public void testHasPrefix() {
    assertThat(InterledgerAddressPrefix.of("g.bob").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("g.bob.foo").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("g.bob.foo").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("self.bob").hasPrefix()).isTrue();

    assertThat(InterledgerAddressPrefix.of("g.1.1").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("private.1.1").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("example.1.1").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("peer.1.1").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("self.1.1").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("test.1.1").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("test1.1.1").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("test2.1.1").hasPrefix()).isTrue();
    assertThat(InterledgerAddressPrefix.of("test3.1.1").hasPrefix()).isTrue();
  }

  @Test
  public void testAddressEqualsHashcode() {
    final InterledgerAddressPrefix addressPrefix1 = InterledgerAddressPrefix.of("g.foo.bob");
    final InterledgerAddressPrefix addressPrefix2 = InterledgerAddressPrefix.of("g.foo.bob");
    final InterledgerAddressPrefix addressPrefix3 = InterledgerAddressPrefix.of("g.foo.bar");

    assertThat(addressPrefix1.hashCode()).isEqualTo(addressPrefix2.hashCode());
    assertThat(addressPrefix1 == addressPrefix2).isTrue();
    assertThat(addressPrefix1).isEqualTo(addressPrefix2);
    assertThat(addressPrefix2).isEqualTo(addressPrefix1);
    assertThat(addressPrefix1.toString()).isEqualTo(addressPrefix2.toString());

    assertThat(addressPrefix1.hashCode()).isNotEqualTo(addressPrefix3.hashCode());
    assertThat(addressPrefix1 == addressPrefix3).isFalse();
    assertThat(addressPrefix1).isNotEqualTo(addressPrefix3);
    assertThat(addressPrefix3).isNotEqualTo(addressPrefix1);
    assertThat(addressPrefix1.toString()).isNotEqualTo(addressPrefix3.toString());
  }

  @Test
  public void testToString() {
    assertThat(InterledgerAddressPrefix.of("g.foo.bob").toString())
        .isEqualTo("InterledgerAddressPrefix{value=g.foo.bob}");

    assertThat(InterledgerAddressPrefix.of(G_FOO).toString())
        .isEqualTo("InterledgerAddressPrefix{value=g.foo}");
  }

  @Test(expected = NullPointerException.class)
  public void testValidateWithNullAddress() {
    InterledgerAddressPrefix.builder().value(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_02() {
    assertValidationErrorThenThrow(".self",
        INVALID_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_04() {
    assertValidationErrorThenThrow(".foo",
        INVALID_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_05() {
    assertValidationErrorThenThrow("gg",
        INVALID_SCHEME_PREFIX,
        "gg");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_06() {
    assertValidationErrorThenThrow(" g",
        INVALID_SCHEME_PREFIX,
        " g");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_07() {
    assertValidationErrorThenThrow("g ",
        INVALID_SCHEME_PREFIX,
        "g ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_08() {
    assertValidationErrorThenThrow("@@@",
        INVALID_SCHEME_PREFIX,
        "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_01() {
    assertValidationErrorThenThrow("g.@@@",
        INVALID_SEGMENT, "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_02() {
    assertValidationErrorThenThrow("g. ",
        INVALID_SEGMENT, " ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_03() {
    assertValidationErrorThenThrow("g.é",
        INVALID_SEGMENT, "é");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_04() {
    assertValidationErrorThenThrow("g.", ILLEGAL_ENDING,
        "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_04b() {
    assertValidationErrorThenThrow("g.foo.",
        ILLEGAL_ENDING, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_05() {
    assertValidationErrorThenThrow("g..bar",
        INVALID_SEGMENT, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_06() {
    assertValidationErrorThenThrow("g.foo.@.baz",
        INVALID_SEGMENT, "@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_07() {
    assertValidationErrorThenThrow("g.foo.bar.a@1",
        INVALID_SEGMENT, "a@1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_08() {
    assertValidationErrorThenThrow("g.foo.bar.baz ",
        INVALID_SEGMENT, "baz ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_09() {
    assertValidationErrorThenThrow("g.foo.bar.baz\r\n",
        INVALID_SEGMENT, "baz\r\n");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithSegmentsUnderflow() {
    assertValidationErrorThenThrow("",
        INVALID_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithLengthOverflow() {
    assertValidationErrorThenThrow(TOO_LONG,
        ADDRESS_OVERFLOW);
  }

  private void assertValidationErrorThenThrow(final String actualAddressString,
      final InterledgerAddressPrefix.AbstractInterledgerAddressPrefix.Error expectedError,
      final Object... errorParams) {
    try {
      InterledgerAddressPrefix.builder().value(actualAddressString).build();
      fail("Should have thrown an IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(expectedError.getMessageFormat(), errorParams));
      throw e;
    } catch (final Exception e) {
      fail("Should have thrown an IllegalArgumentException");
      throw e;
    }
  }

  ////////////////////
  // test getRootPrefix
  ////////////////////

  @Test
  public void getRootPrefix() {
    assertThat(InterledgerAddressPrefix.of("g.baz").getRootPrefix().getValue()).isEqualTo("g");
    assertThat(InterledgerAddressPrefix.of("g.baz.bool").getRootPrefix().getValue()).isEqualTo("g");
    assertThat(InterledgerAddressPrefix.of("g.baz.bool.foo").getRootPrefix().getValue()).isEqualTo("g");
    assertThat(InterledgerAddressPrefix.of("g.baz.bool.foo.bar.boo").getRootPrefix().getValue()).isEqualTo("g");
    assertThat(InterledgerAddressPrefix.of("g").getRootPrefix().getValue()).isEqualTo("g");
  }

  ////////////////////
  // test from InterledgerAddress
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testFromAddressWithNull() {
    try {
      InterledgerAddressPrefix.from((InterledgerAddress) null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
      throw e;
    }
  }

  @Test
  public void testFromAddressWithAddress() {
    final InterledgerAddressPrefix prefix = InterledgerAddressPrefix
        .from(InterledgerAddress.of(G_FOO));
    assertThat(prefix.getValue()).isEqualTo(G_FOO);
  }

  ////////////////////
  // test from AllocationScheme
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testFromAllocationSchemeWithNull() {
    try {
      InterledgerAddressPrefix.from((AllocationScheme) null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
      throw e;
    }
  }

  @Test
  public void testFromAddressWithAllocationScheme() {
    final InterledgerAddressPrefix allocationScheme = InterledgerAddressPrefix
        .from(AllocationScheme.GLOBAL);
    assertThat(allocationScheme).isEqualTo(InterledgerAddressPrefix.GLOBAL);
  }

  @Test
  public void testPrefixesWithAllocationScheme() {
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.GLOBAL)).isEqualTo(InterledgerAddressPrefix.GLOBAL);
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.EXAMPLE)).isEqualTo(InterledgerAddressPrefix.EXAMPLE);
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.PRIVATE)).isEqualTo(InterledgerAddressPrefix.PRIVATE);
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.PEER)).isEqualTo(InterledgerAddressPrefix.PEER);
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.SELF)).isEqualTo(InterledgerAddressPrefix.SELF);
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.TEST)).isEqualTo(InterledgerAddressPrefix.TEST);
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.TEST1)).isEqualTo(InterledgerAddressPrefix.TEST1);
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.TEST2)).isEqualTo(InterledgerAddressPrefix.TEST2);
    assertThat(InterledgerAddressPrefix.from(AllocationScheme.TEST3)).isEqualTo(InterledgerAddressPrefix.TEST3);
  }
}
