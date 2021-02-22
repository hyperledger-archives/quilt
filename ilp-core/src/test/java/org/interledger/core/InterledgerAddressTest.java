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
import static org.assertj.core.api.Assertions.fail;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error.ADDRESS_OVERFLOW;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error.ILLEGAL_ENDING;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error.INVALID_SEGMENT;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error.MISSING_SCHEME_PREFIX;

import org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error;
import org.interledger.core.InterledgerAddress.AllocationScheme;

import org.junit.Test;

/**
 * JUnit tests to test {@link InterledgerAddress}.
 */
public class InterledgerAddressTest {

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

  ////////////////////////
  // Builder Tests
  ////////////////////////

  @Test(expected = IllegalStateException.class)
  public void testConstructorWithUninitializedBuild() {
    String errorMessage = "Cannot build InterledgerAddress, some of required attributes are not set [value]";
    try {
      InterledgerAddress.builder().build();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo(errorMessage);
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void testBuilderWithNullValue() {
    try {
      InterledgerAddress.builder()
        .value(null)
        .build();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("value");
      throw e;
    }
  }

  @Test
  public void testConstructionDeliverableAddress() {
    final InterledgerAddress address = InterledgerAddress.builder().value(TEST1_US_USD_BOB).build();
    assertThat(address.getValue()).isEqualTo(TEST1_US_USD_BOB);
  }

  @Test
  public void testConstructionLedgerPrefix() {
    final InterledgerAddress address = InterledgerAddress.builder().value(TEST1_US_USD).build();
    assertThat(address.getValue()).isEqualTo(TEST1_US_USD);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyAddress() {
    try {
      InterledgerAddress.builder().value("").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(MISSING_SCHEME_PREFIX.getMessageFormat());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBlankAddress() {
    try {
      InterledgerAddress.builder().value("  ").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(Error.INVALID_SCHEME_PREFIX.getMessageFormat(), "  "));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressWithSpace() {
    try {
      InterledgerAddress.builder().value(TEST1_US_USD_BOB + " space").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SEGMENT.getMessageFormat(), "bob space"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressTooLong() {
    try {
      InterledgerAddress.builder().value(TOO_LONG).build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(ADDRESS_OVERFLOW.getMessageFormat());
      throw e;
    }
  }

  @Test
  public void testAddressJustRightLength() {
    // This is 1023 characters long...
    final InterledgerAddress address = InterledgerAddress.builder().value(JUST_RIGHT).build();
    assertThat(address.getValue()).isEqualTo(JUST_RIGHT);
  }

  @Test
  public void testAddressAllValidCharacters() {
    final String allValues = "g.0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_~.-";
    final InterledgerAddress address = InterledgerAddress.builder().value(allValues).build();
    assertThat(address.getValue()).isEqualTo(allValues);
  }

  ////////////////////////
  // Accessors
  ////////////////////////

  @Test
  public void testValue() {
    assertThat(InterledgerAddress.of("g.foo.bob").getValue()).isEqualTo("g.foo.bob");
  }

  @Test(expected = NullPointerException.class)
  public void testOfWithNull() {
    try {
      InterledgerAddress.of(null);
      fail("should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("value must not be null!");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOfWithEmptyString() {
    try {
      InterledgerAddress.of("");
      fail("should have thrown an exception but did not!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("InterledgerAddress does not start with a scheme prefix");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOfWithBlankString() {
    try {
      InterledgerAddress.of(" ");
      fail("should have thrown an exception but did not!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("The ' ' AllocationScheme is invalid!");
      throw e;
    }
  }

  @Test
  public void testStartsWithString() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith("g")).isTrue();
    assertThat(address.startsWith("g")).isTrue();
    assertThat(address.startsWith("g.foo")).isTrue();
    assertThat(address.startsWith("g.foo")).isTrue();
    assertThat(address.startsWith("g.foo.bob")).isTrue();
    assertThat(address.startsWith("test.foo.bob")).isFalse();
  }

  @Test
  public void testStartsWithInterledgerAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith(InterledgerAddress.of("g.foo"))).isTrue();
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.bob"))).isTrue();
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.bob.bar"))).isFalse();
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.bobbar"))).isFalse();
    assertThat(address.startsWith(InterledgerAddress.of("test1.foo.bob"))).isFalse();
  }

  @Test
  public void testStartsWithInterledgerAddressPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g"))).isTrue();
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo"))).isTrue();
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo.bob"))).isTrue();
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo.bar"))).isFalse();
    assertThat(address.startsWith(InterledgerAddressPrefix.of("test.foo"))).isFalse();

    final InterledgerAddress smallAddress = InterledgerAddress.of("g.foo");
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("g"))).isTrue();
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("g.foo"))).isTrue();
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("g.foo.bob"))).isFalse();
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("g.foo.bar"))).isFalse();
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("test.foo"))).isFalse();
  }

  @Test(expected = NullPointerException.class)
  public void testAddressPrefixStartsWithNull() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo");
    InterledgerAddressPrefix addressPrefix = null;
    try {
      address.startsWith(addressPrefix); // addressPrefix will be null
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("addressPrefix must not be null!");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressPrefixStartsWithEmpty() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    InterledgerAddressPrefix addressPrefix = InterledgerAddressPrefix.of("");
    try {
      address.startsWith(addressPrefix);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("addressPrefix must not be empty!");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void testAddressWithNull() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    try {
      addressPrefix.with(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("addressSegment must not be null!");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressWithEmpty() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    try {
      addressPrefix.with("");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(ILLEGAL_ENDING.getMessageFormat());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressWithBlank() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    try {
      addressPrefix.with("  ");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(INVALID_SEGMENT.getMessageFormat(), "  "));
      throw e;
    }
  }

  /**
   * Validates adding an address prefix (as an InterledgerAddress) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithAddressPrefix() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue()).isEqualTo("g.foo.bob.boz");
  }

  /**
   * Validates adding a destination address (as an InterledgerAddress) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithDestinationAddress() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    final String additionalDestinationAddress = "bob";
    assertThat(addressPrefix.with(additionalDestinationAddress).getValue()).isEqualTo("g.foo.bob");
  }

  /**
   * Validates adding a destination address (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testDestinationAddressWithDestinationAddress() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue()).isEqualTo("g.foo.bob.boz");
  }

  /**
   * Validates adding an address prefix  (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testAddressWithAddress() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue()).isEqualTo("g.foo.bob.boz");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDestinationAddressWithoutEnoughSegments() {
    try {
      InterledgerAddress.of("g");
      fail("Should have failed and been caught as an exception");
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(Error.SEGMENTS_UNDERFLOW.getMessageFormat());
      throw e;
    }
  }

  @Test
  public void testInterledgerAddressCreationWithAllocationScheme() {
    InterledgerAddress addressUsingGlobalAllocationScheme = AllocationScheme.GLOBAL.with("bob");
    InterledgerAddress addressUsingOfInterledgerAddress = InterledgerAddress.of("g.bob");

    assertThat(addressUsingGlobalAllocationScheme).isEqualTo(addressUsingOfInterledgerAddress);
  }

  @Test
  public void testGetPrefixFromShortPrefix() {
    assertThat(InterledgerAddress.of("g.1").getPrefix().getValue().isEmpty()).isFalse();
    assertThat(InterledgerAddress.of("g.1").getPrefix()).isEqualTo(InterledgerAddressPrefix.of("g"));
  }

  @Test
  public void testGetPrefixFromPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.example");
    assertThat(address.getPrefix().getValue().isEmpty()).isFalse();
    assertThat(address.getPrefix()).isEqualTo(InterledgerAddressPrefix.of("g"));
  }

  @Test
  public void testGetPrefixFromLongPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().getValue()).isEqualTo("g.alpha.beta.charlie.delta");
  }

  @Test
  public void testGetPrefixFromAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.example.bob");
    assertThat(address.getPrefix().getValue()).isEqualTo("g.example");
  }

  @Test
  public void testGetPrefixFromShortAddress() {
    assertThat(InterledgerAddress.of("g.bob.foo").getPrefix().getValue()).isEqualTo("g.bob");
    assertThat(InterledgerAddress.of("g.b.f").getPrefix().getValue()).isEqualTo("g.b");
  }

  @Test
  public void testGetPrefixFromLongAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().getValue()).isEqualTo("g.alpha.beta.charlie.delta");
  }

  @Test
  public void testAddressEqualsHashcode() {
    final InterledgerAddress addressPrefix1 = InterledgerAddress.of("g.foo.bob");
    final InterledgerAddress addressPrefix2 = InterledgerAddress.of("g.foo.bob");
    final InterledgerAddress addressPrefix3 = InterledgerAddress.of("g.foo.bar");

    assertThat(addressPrefix1.hashCode()).isEqualTo(addressPrefix2.hashCode());
    // As of https://github.com/hyperledger/quilt/issues/418, this should NOT be true, as InterledgerAddresses
    // are not interned
    assertThat(addressPrefix1 == addressPrefix2).isFalse();
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
    assertThat(InterledgerAddress.of("g.foo.bob").toString()).isEqualTo("InterledgerAddress{value=g.foo.bob}");
    assertThat(InterledgerAddress.of("g.foo").toString()).isEqualTo("InterledgerAddress{value=g.foo}");
  }

  @Test(expected = NullPointerException.class)
  public void testValidateWithNullAddress() {
    InterledgerAddress.builder().value(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefixContaining02() {
    assertValidationErrorThenThrow(".self", Error.INVALID_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefixContaining04() {
    assertValidationErrorThenThrow(".foo", Error.INVALID_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefixContaining05() {
    assertValidationErrorThenThrow("gg", Error.INVALID_SCHEME_PREFIX, "gg");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefixContaining06() {
    assertValidationErrorThenThrow(" g", Error.INVALID_SCHEME_PREFIX, " g");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefixContaining07() {
    assertValidationErrorThenThrow("g ", Error.INVALID_SCHEME_PREFIX, "g ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefixContaining08() {
    assertValidationErrorThenThrow("@@@", Error.INVALID_SCHEME_PREFIX, "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining01() {
    assertValidationErrorThenThrow("g.@@@", Error.INVALID_SEGMENT, "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining02() {
    assertValidationErrorThenThrow("g. ", Error.INVALID_SEGMENT, " ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining03() {
    assertValidationErrorThenThrow("g.é", Error.INVALID_SEGMENT, "é");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining04() {
    assertValidationErrorThenThrow("g.", Error.ILLEGAL_ENDING, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining04b() {
    assertValidationErrorThenThrow("g.foo.", Error.ILLEGAL_ENDING, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining05() {
    assertValidationErrorThenThrow("g..bar", Error.INVALID_SEGMENT, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining06() {
    assertValidationErrorThenThrow("g.foo.@.baz", Error.INVALID_SEGMENT, "@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining07() {
    assertValidationErrorThenThrow("g.foo.bar.a@1", Error.INVALID_SEGMENT, "a@1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining08() {
    assertValidationErrorThenThrow("g.foo.bar.baz ", Error.INVALID_SEGMENT, "baz ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegmentContaining09() {
    assertValidationErrorThenThrow("g.foo.bar.baz\r\n", Error.INVALID_SEGMENT, "baz\r\n");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithSegmentsUnderflow() {
    assertValidationErrorThenThrow("g", Error.SEGMENTS_UNDERFLOW);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithLengthOverflow() {
    assertValidationErrorThenThrow(TOO_LONG, Error.ADDRESS_OVERFLOW);
  }

  @Test
  public void testLastSegement() {
    assertThat(InterledgerAddress.of("g.foo").lastSegment()).isEqualTo("foo");
    assertThat(InterledgerAddress.of("g.foo.bar").lastSegment()).isEqualTo("bar");
    assertThat(InterledgerAddress.of("g.foo.bar.baz").lastSegment()).isEqualTo("baz");
    assertThat(InterledgerAddress.of("g.foo.bar.baz.b.boo.glub.goober").lastSegment()).isEqualTo("goober");
  }

  private void assertValidationErrorThenThrow(final String actualAddressString,
    final Error expectedError, final Object... errorParams) {
    try {
      InterledgerAddress.builder().value(actualAddressString).build();
      fail("Should have thrown an IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(String.format(expectedError.getMessageFormat(), errorParams));
      throw e;
    } catch (final Exception e) {
      fail("Should have thrown an IllegalArgumentException");
      throw e;
    }
  }
}
