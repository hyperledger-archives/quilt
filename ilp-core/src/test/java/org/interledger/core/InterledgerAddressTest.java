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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
  public void test_constructor_wit_uninitialized_build() {
    try {
      InterledgerAddress.builder().build();
    } catch (IllegalStateException e) {
      //Removed message check. This exception is raised by the Immutable generated code.
      //assertThat(e.getMessage(), is("value must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void test_wither_with_null_value() {
    try {
      InterledgerAddress.builder()
          .value(null)
          .build();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("value"));
      throw e;
    }
  }

  @Test
  public void testConstruction_DeliverableAddress() {
    final InterledgerAddress address = InterledgerAddress.builder().value(TEST1_US_USD_BOB).build();
    assertThat(address.value(), is(TEST1_US_USD_BOB));
  }

  @Test
  public void testConstruction_LedgerPrefix() {
    final InterledgerAddress address = InterledgerAddress.builder().value(TEST1_US_USD).build();
    assertThat(address.value(), is(TEST1_US_USD));
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_empty_address() {
    try {
      InterledgerAddress.builder().value("").build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(MISSING_SCHEME_PREFIX.getMessageFormat()));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_blank_address() {
    try {
      InterledgerAddress.builder().value("  ").build();
    } catch (IllegalArgumentException e) {
      assertThat(
          e.getMessage(),
          is(String.format(Error.INVALID_SCHEME_PREFIX.getMessageFormat(), "  "))
      );
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_address_with_space() {
    try {
      InterledgerAddress.builder().value(TEST1_US_USD_BOB + " space").build();
    } catch (IllegalArgumentException e) {
      assertThat(
          e.getMessage(),
          is(String.format(INVALID_SEGMENT.getMessageFormat(), "bob space"))
      );
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_address_too_long() {
    try {
      InterledgerAddress.builder().value(TOO_LONG).build();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(ADDRESS_OVERFLOW.getMessageFormat()));
      throw e;
    }
  }

  @Test
  public void test_address_just_right_length() {
    // This is 1023 characters long...
    final InterledgerAddress address = InterledgerAddress.builder().value(JUST_RIGHT).build();
    assertThat(address.value(), is(JUST_RIGHT));
  }

  @Test
  public void test_address_all_valid_characters() {
    final String allValues = "g.0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_~.-";
    final InterledgerAddress address = InterledgerAddress.builder().value(allValues).build();
    assertThat(address.value(), is(allValues));
  }

  ////////////////////////
  // Accessors
  ////////////////////////

  @Test
  public void testValue() {
    assertThat(InterledgerAddress.of("g.foo.bob").value(), is("g.foo.bob"));
  }

  @Test
  public void testAllocationScheme() {
    assertThat(
        InterledgerAddress.of("g.foo.bob").allocationScheme(),
        is(AllocationScheme.builder().value("g").build())
    );
  }

  @Test
  public void testStartsWithString() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith("g"), is(true));
    assertThat(address.startsWith("g"), is(true));
    assertThat(address.startsWith("g.foo"), is(true));
    assertThat(address.startsWith("g.foo"), is(true));
    assertThat(address.startsWith("g.foo.bob"), is(true));
    assertThat(address.startsWith("test.foo.bob"), is(false));
  }

  @Test
  public void testStartsWithInterledgerAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith(InterledgerAddress.of("g.foo")), is(true));
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.bob")), is(true));
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.bob.bar")), is(false));
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.bobbar")), is(false));
    assertThat(address.startsWith(InterledgerAddress.of("test1.foo.bob")), is(false));
  }

  @Test(expected = NullPointerException.class)
  public void testAddressWithNull() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    try {
      addressPrefix.with(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("addressSegment must not be null!"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressWithEmpty() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    try {
      addressPrefix.with("");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(ILLEGAL_ENDING.getMessageFormat()));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressWithBlank() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    try {
      addressPrefix.with("  ");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(String.format(INVALID_SEGMENT.getMessageFormat(), "  ")));
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
    assertThat(destinationAddress.with(additionalDestinationAddress).value(),
        is("g.foo.bob.boz"));
  }

  /**
   * Validates adding a destination address (as an InterledgerAddress) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithDestinationAddress() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    final String additionalDestinationAddress = "bob";
    assertThat(addressPrefix.with(additionalDestinationAddress).value(), is("g.foo.bob"));
  }

  /**
   * Validates adding a destination address (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testDestinationAddressWithDestinationAddress() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).value(),
        is("g.foo.bob.boz"));
  }

//  @Test
//  public void testGetPrefixRoot() {
//    assertThat(InterledgerAddress.of("g").getPrefix().value(), is("g"));
//    assertThat(InterledgerAddress.of("self").getPrefix().value(), is("self"));
//    assertThat(InterledgerAddress.of("test").getPrefix().value(), is("test"));
//    assertThat(InterledgerAddress.of("test1").getPrefix().value(), is("test1"));
//    assertThat(InterledgerAddress.of("test2").getPrefix().value(), is("test2"));
//    assertThat(InterledgerAddress.of("test3").getPrefix().value(), is("test3"));
//  }

  /**
   * Validates adding an address prefix  (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testAddressWithAddress() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).value(),
        is("g.foo.bob.boz"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDestinationAddressWithoutEnoughSegments() {
    try {
      InterledgerAddress.of("g");
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage(), is(Error.SEGMENTS_UNDERFLOW.getMessageFormat()));
      throw e;
    }
  }

//  @Test
//  public void testGetPrefixFromAllocationScheme() {
//    assertThat(InterledgerAddress.of("g").getPrefix().isPresent(), is(false));
//  }

  @Test
  public void testGetPrefixFromShortPrefix() {
    assertThat(InterledgerAddress.of("g.1").getPrefix().isPresent(), is(false));
  }

  @Test
  public void testGetPrefixFromPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.example");
    assertThat(address.getPrefix().isPresent(), is(false));
  }

  @Test
  public void testGetPrefixFromLongPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().get().value(), is("g.alpha.beta.charlie.delta"));
  }

  @Test
  public void testGetPrefixFromAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.example.bob");
    assertThat(address.getPrefix().get().value(), is("g.example"));
  }

  @Test
  public void testGetPrefixFromShortAddress() {
    assertThat(InterledgerAddress.of("g.bob.foo").getPrefix().get().value(), is("g.bob"));
    assertThat(InterledgerAddress.of("g.b.f").getPrefix().get().value(), is("g.b"));
  }

  @Test
  public void testGetPrefixFromLongAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().get().value(), is("g.alpha.beta.charlie.delta"));
  }

//  @Test
//  public void testGetPrefix() {
//    assertThat(InterledgerAddress.of("g.bob").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("g.bob.foo").getPrefix().get().value(), is("g.bob"));
//    assertThat(
//        InterledgerAddress.of("g.bob.foo").getPrefix().get().value(), is("g.bob")
//    );
//    assertThat(InterledgerAddress.of("g").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("private").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("example").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("peer").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("self").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("test").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("test1").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("test2").getPrefix().isPresent(), is(false));
//    assertThat(InterledgerAddress.of("test3").getPrefix().isPresent(), is(false));
//  }

  @Test
  public void testHasPrefix() {
    assertThat(InterledgerAddress.of("g.bob").hasPrefix(), is(false));
    assertThat(InterledgerAddress.of("g.bob.foo").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("g.bob.foo").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("self.bob").hasPrefix(), is(false));

    assertThat(InterledgerAddress.of("g.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("private.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("example.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("peer.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("self.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("test.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("test1.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("test2.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("test3.1.1").hasPrefix(), is(true));

    //assertThat(InterledgerAddress.of("g").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("private").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("example").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("peer").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("self").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("test").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("test1").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("test2").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("test3").hasPrefix(), is(false));
  }

  // TODO: Add to AllocationScheme Tests...
//  @Test
//  public void testIsRootPrefix() {
//    assertThat(InterledgerAddress.of("g").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("private").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("example").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("peer").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("self").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("test").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("test1").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("test2").hasPrefix(), is(false));
//    assertThat(InterledgerAddress.of("test3").hasPrefix(), is(false));
//  }

  @Test
  public void testAddressEqualsHashcode() {
    final InterledgerAddress addressPrefix1 = InterledgerAddress.of("g.foo.bob");
    final InterledgerAddress addressPrefix2 = InterledgerAddress.of("g.foo.bob");
    final InterledgerAddress addressPrefix3 = InterledgerAddress.of("g.foo.bar");

    assertThat(addressPrefix1.hashCode() == addressPrefix2.hashCode(), is(true));
    assertThat(addressPrefix1 == addressPrefix2, is(true));
    assertThat(addressPrefix1.equals(addressPrefix2), is(true));
    assertThat(addressPrefix2.equals(addressPrefix1), is(true));
    assertThat(addressPrefix1.toString().equals(addressPrefix2.toString()), is(true));

    assertThat(addressPrefix1.hashCode() == addressPrefix3.hashCode(), is(false));
    assertThat(addressPrefix1 == addressPrefix3, is(false));
    assertThat(addressPrefix1.equals(addressPrefix3), is(false));
    assertThat(addressPrefix3.equals(addressPrefix1), is(false));
    assertThat(addressPrefix1.toString().equals(addressPrefix3.toString()), is(false));
  }

  @Test
  public void testToString() {
    assertThat(InterledgerAddress.of("g.foo.bob").toString(),
        is("InterledgerAddress{value=g.foo.bob}"));
    assertThat(InterledgerAddress.of("g.foo").toString(),
        is("InterledgerAddress{value=g.foo}"));
  }

//  @Test
//  public void testIsSchemePrefix() {
//    assertThat(addressParser.isSchemePrefix("self"), is(false));
//    assertThat(addressParser.isSchemePrefix("self"), is(true));
//    assertThat(addressParser.isSchemePrefix("g"), is(true));
//    assertThat(addressParser.isSchemePrefix("g.foo"), is(false));
//    assertThat(addressParser.isSchemePrefix("g.foo"), is(false));
//    assertThat(addressParser.isSchemePrefix("g.g"), is(false));
//    assertThat(addressParser.isSchemePrefix("g.foo.bar"), is(false));
//    assertThat(addressParser.isSchemePrefix(JUST_RIGHT), is(false));
//  }
//
//  @Test(expected = NullPointerException.class)
//  public void testIsSchemePrefixWithNullAddress() {
//    assertThat(addressParser.isSchemePrefix(null), is(false));
//  }

//  @Test
//  public void testIsSchemePrefixWithEmptyAddress() {
//    assertThat(addressParser.isSchemePrefix(""), is(false));
//  }

//  @Test
//  public void testIsSchemePrefixWithInvalidAddresses() {
//    assertThat(addressParser.isSchemePrefix("g"), is(false));
//    assertThat(addressParser.isSchemePrefix("self"), is(false));
//    assertThat(addressParser.isSchemePrefix(""), is(false));
//    assertThat(addressParser.isSchemePrefix(".self"), is(false));
//    assertThat(addressParser.isSchemePrefix(".foo"), is(false));
//    assertThat(addressParser.isSchemePrefix(".@@@"), is(false));
//    assertThat(addressParser.isSchemePrefix("gg"), is(false));
//    assertThat(addressParser.isSchemePrefix(" g"), is(false));
//    assertThat(addressParser.isSchemePrefix("g "), is(false));
//    assertThat(addressParser.isSchemePrefix("g. "), is(false));
//    assertThat(addressParser.isSchemePrefix("g.\r\n"), is(false));
//    assertThat(addressParser.isSchemePrefix("foo"), is(false));
//    assertThat(addressParser.isSchemePrefix("@@@"), is(false));
//    assertThat(addressParser.isSchemePrefix("g.foo"), is(false));
//  }
//
//  @Test
//  public void testValidate() {
//    addressParser.validate("self");
//    addressParser.validate("g");
//    addressParser.validate("g.foo");
//    addressParser.validate("g.g");
//    addressParser.validate("g.foo.bar");
//    addressParser.validate("g.foo.bar");
//    addressParser.validate("g.foo.bar.baz");
//    addressParser.validate(JUST_RIGHT);
//  }

//  @Test(expected = NullPointerException.class)
//  public void testValidateWithNullAddress() {
//    addressParser.validate(null);
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithEmptyAddress() {
//    assertValidationErrorThenThrow("", InterledgerAddressParser.Error.MISSING_SCHEME_PREFIX);
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSchemePrefix_01() {
//    assertValidationErrorThenThrow("g", InterledgerAddressParser.Error.MISSING_SCHEME_PREFIX);
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSchemePrefix_02() {
//    assertValidationErrorThenThrow("self", InterledgerAddressParser.Error.MISSING_SCHEME_PREFIX);
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSchemePrefix_03() {
//    assertValidationErrorThenThrow("", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX, "");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSchemePrefix_04() {
//    assertValidationErrorThenThrow(".foo", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX,
//        "");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSchemePrefix_05() {
//    assertValidationErrorThenThrow("gg", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX,
//        "gg");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSchemePrefix_06() {
//    assertValidationErrorThenThrow(" g", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX,
//        " g");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSchemePrefix_07() {
//    assertValidationErrorThenThrow("g ", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX,
//        "g ");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSchemePrefix_08() {
//    assertValidationErrorThenThrow("@@@", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX,
//        "@@@");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_01() {
//    assertValidationErrorThenThrow("g.@@@", InterledgerAddressParser.Error.INVALID_SEGMENT, "@@@");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_02() {
//    assertValidationErrorThenThrow("g. ", InterledgerAddressParser.Error.INVALID_SEGMENT,
//        " ");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_03() {
//    assertValidationErrorThenThrow("g.é", InterledgerAddressParser.Error.INVALID_SEGMENT,
//        "é");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_04() {
//    assertValidationErrorThenThrow("g.", InterledgerAddressParser.Error.INVALID_SEGMENT, "");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_05() {
//    assertValidationErrorThenThrow("g..bar", InterledgerAddressParser.Error.INVALID_SEGMENT, "");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_06() {
//    assertValidationErrorThenThrow("g.foo.@.baz", InterledgerAddressParser.Error.INVALID_SEGMENT,
//        "@");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_07() {
//    assertValidationErrorThenThrow("g.foo.bar.a@1", InterledgerAddressParser.Error.INVALID_SEGMENT,
//        "a@1");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_08() {
//    assertValidationErrorThenThrow("g.foo.bar.baz ", InterledgerAddressParser.Error.INVALID_SEGMENT,
//        "baz ");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithInvalidSegment_09() {
//    assertValidationErrorThenThrow("g.foo.bar.baz\r\n",
//        InterledgerAddressParser.Error.INVALID_SEGMENT,
//        "baz\r\n");
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithSegmentsUnderflow() {
//    assertValidationErrorThenThrow("g.foo", InterledgerAddressParser.Error.SEGMENTS_UNDERFLOW);
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testValidateWithLengthOverflow() {
//    assertValidationErrorThenThrow(TOO_LONG, InterledgerAddressParser.Error.SEGMENTS_UNDERFLOW.getMessageFormat());
//  }
//
//  private void assertValidationErrorThenThrow(final String actualAddressString,
//      final InterledgerAddressParser.Error expectedError, final Object... errorParams) {
//    try {
//      addressParser.validate(actualAddressString);
//      fail("Should have thrown an IllegalArgumentException");
//    } catch (final IllegalArgumentException e) {
//      assertThat(e.getMessage(), is(String.format(expectedError.getMessageFormat(), errorParams)));
//      throw e;
//    } catch (final Exception e) {
//      fail("Should have thrown an IllegalArgumentException");
//      throw e;
//    }
//  }


}
