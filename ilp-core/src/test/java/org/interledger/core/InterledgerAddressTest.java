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
import static org.junit.Assert.fail;

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
      assertThat(e.getMessage(),
          is("Cannot build InterledgerAddress, some of required attributes are not set [value]"));
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
    assertThat(address.getValue(), is(TEST1_US_USD_BOB));
  }

  @Test
  public void testConstruction_LedgerPrefix() {
    final InterledgerAddress address = InterledgerAddress.builder().value(TEST1_US_USD).build();
    assertThat(address.getValue(), is(TEST1_US_USD));
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
    assertThat(address.getValue(), is(JUST_RIGHT));
  }

  @Test
  public void test_address_all_valid_characters() {
    final String allValues = "g.0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_~.-";
    final InterledgerAddress address = InterledgerAddress.builder().value(allValues).build();
    assertThat(address.getValue(), is(allValues));
  }

  ////////////////////////
  // Accessors
  ////////////////////////

  @Test
  public void testValue() {
    assertThat(InterledgerAddress.of("g.foo.bob").getValue(), is("g.foo.bob"));
  }

  @Test
  public void testAllocationScheme() {
    assertThat(
        InterledgerAddress.of("g.foo.bob").getAllocationScheme(),
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

  @Test
  public void testStartsWithInterledgerAddressPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g")), is(true));
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo")), is(true));
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo.bob")), is(true));
    assertThat(address.startsWith(InterledgerAddressPrefix.of("g.foo.bar")), is(false));
    assertThat(address.startsWith(InterledgerAddressPrefix.of("test.foo")), is(false));

    final InterledgerAddress smallAddress = InterledgerAddress.of("g.foo");
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("g")), is(true));
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("g.foo")), is(true));
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("g.foo.bob")), is(false));
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("g.foo.bar")), is(false));
    assertThat(smallAddress.startsWith(InterledgerAddressPrefix.of("test.foo")), is(false));
  }

  @Test(expected = NullPointerException.class)
  public void testAddressPrefixStartsWithNull() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo");
    InterledgerAddressPrefix addressPrefix = null;
    try {
      address.startsWith(addressPrefix); // addressPrefix will be null
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("addressPrefix must not be null!"));
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
      assertThat(e.getMessage(), is("addressPrefix must not be empty!"));
      throw e;
    }
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
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob.boz"));
  }

  /**
   * Validates adding a destination address (as an InterledgerAddress) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithDestinationAddress() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo");
    final String additionalDestinationAddress = "bob";
    assertThat(addressPrefix.with(additionalDestinationAddress).getValue(), is("g.foo.bob"));
  }

  /**
   * Validates adding a destination address (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testDestinationAddressWithDestinationAddress() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob.boz"));
  }

  /**
   * Validates adding an address prefix  (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testAddressWithAddress() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob.boz"));
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

  @Test
  public void testInterledgerAddressCreationWithAllocationScheme() {
    InterledgerAddress addressUsingGlobalAllocationScheme = AllocationScheme.GLOBAL.with("bob");
    InterledgerAddress addressUsingOfInterledgerAddress = InterledgerAddress.of("g.bob");

    assertThat(addressUsingGlobalAllocationScheme, is(addressUsingOfInterledgerAddress));
  }

  @Test
  public void testInterledgerAddressCreationWithCorrectAllocationScheme() {
    InterledgerAddress globalAddress = AllocationScheme.GLOBAL.with("bob.baz");
    InterledgerAddress exampleAddress = AllocationScheme.EXAMPLE.with("bob.baz");
    InterledgerAddress peerAddress = AllocationScheme.PEER.with("bob.baz");
    InterledgerAddress privateAddress = AllocationScheme.PRIVATE.with("bob.baz");
    InterledgerAddress selfAddress = AllocationScheme.SELF.with("bob.baz");
    InterledgerAddress testAddress = AllocationScheme.TEST.with("bob.baz");
    InterledgerAddress test1Address = AllocationScheme.TEST1.with("bob.baz");
    InterledgerAddress test2Address = AllocationScheme.TEST2.with("bob.baz");
    InterledgerAddress test3Address = AllocationScheme.TEST3.with("bob.baz");

    assertThat(globalAddress.getAllocationScheme(), is(AllocationScheme.GLOBAL));
    assertThat(globalAddress.getValue(), is("g.bob.baz"));

    assertThat(exampleAddress.getAllocationScheme(), is(AllocationScheme.EXAMPLE));
    assertThat(exampleAddress.getValue(), is("example.bob.baz"));

    assertThat(peerAddress.getAllocationScheme(), is(AllocationScheme.PEER));
    assertThat(peerAddress.getValue(), is("peer.bob.baz"));

    assertThat(privateAddress.getAllocationScheme(), is(AllocationScheme.PRIVATE));
    assertThat(privateAddress.getValue(), is("private.bob.baz"));

    assertThat(selfAddress.getAllocationScheme(), is(AllocationScheme.SELF));
    assertThat(selfAddress.getValue(), is("self.bob.baz"));

    assertThat(testAddress.getAllocationScheme(), is(AllocationScheme.TEST));
    assertThat(testAddress.getValue(), is("test.bob.baz"));

    assertThat(test1Address.getAllocationScheme(), is(AllocationScheme.TEST1));
    assertThat(test1Address.getValue(), is("test1.bob.baz"));

    assertThat(test2Address.getAllocationScheme(), is(AllocationScheme.TEST2));
    assertThat(test2Address.getValue(), is("test2.bob.baz"));

    assertThat(test3Address.getAllocationScheme(), is(AllocationScheme.TEST3));
    assertThat(test3Address.getValue(), is("test3.bob.baz"));
  }

  @Test(expected = NullPointerException.class)
  public void testInterledgerAddressCreationWithNull() {
    try {
      AllocationScheme.GLOBAL.with(null);
      fail("should have failed and the error been caught");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("value must not be null!"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInterledgerAddressCreationWithEmpty() {
    try {
      AllocationScheme.GLOBAL.with("");
      fail("should have failed and the error been caught");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(ILLEGAL_ENDING.getMessageFormat()));
      throw e;
    }
  }

  @Test
  public void testGetPrefixFromShortPrefix() {
    assertThat(InterledgerAddress.of("g.1").getPrefix().isPresent(), is(true));
    assertThat(InterledgerAddress.of("g.1").getPrefix().get(), is(InterledgerAddressPrefix.of("g")));
  }

  @Test
  public void testGetPrefixFromPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.example");
    assertThat(address.getPrefix().isPresent(), is(true));
    assertThat(address.getPrefix().get(), is(InterledgerAddressPrefix.of("g")));
  }

  @Test
  public void testGetPrefixFromLongPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().get().getValue(), is("g.alpha.beta.charlie.delta"));
  }

  @Test
  public void testGetPrefixFromAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.example.bob");
    assertThat(address.getPrefix().get().getValue(), is("g.example"));
  }

  @Test
  public void testGetPrefixFromShortAddress() {
    assertThat(InterledgerAddress.of("g.bob.foo").getPrefix().get().getValue(), is("g.bob"));
    assertThat(InterledgerAddress.of("g.b.f").getPrefix().get().getValue(), is("g.b"));
  }

  @Test
  public void testGetPrefixFromLongAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().get().getValue(), is("g.alpha.beta.charlie.delta"));
  }

  @Test
  public void testHasPrefix() {
    assertThat(InterledgerAddress.of("g.bob").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("g.bob.foo").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("g.bob.foo").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("self.bob").hasPrefix(), is(true));

    assertThat(InterledgerAddress.of("g.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("private.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("example.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("peer.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("self.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("test.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("test1.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("test2.1.1").hasPrefix(), is(true));
    assertThat(InterledgerAddress.of("test3.1.1").hasPrefix(), is(true));
  }

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
    assertThat(InterledgerAddress.of("g.foo.bob").toString(), is("InterledgerAddress{value=g.foo.bob}"));
    assertThat(InterledgerAddress.of("g.foo").toString(), is("InterledgerAddress{value=g.foo}"));
  }

  @Test(expected = NullPointerException.class)
  public void testValidateWithNullAddress() {
    InterledgerAddress.builder().value(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_02() {
    assertValidationErrorThenThrow(".self", Error.INVALID_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_04() {
    assertValidationErrorThenThrow(".foo", Error.INVALID_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_05() {
    assertValidationErrorThenThrow("gg", Error.INVALID_SCHEME_PREFIX, "gg");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_06() {
    assertValidationErrorThenThrow(" g", Error.INVALID_SCHEME_PREFIX, " g");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_07() {
    assertValidationErrorThenThrow("g ", Error.INVALID_SCHEME_PREFIX, "g ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_08() {
    assertValidationErrorThenThrow("@@@", Error.INVALID_SCHEME_PREFIX, "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_01() {
    assertValidationErrorThenThrow("g.@@@", Error.INVALID_SEGMENT, "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_02() {
    assertValidationErrorThenThrow("g. ", Error.INVALID_SEGMENT, " ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_03() {
    assertValidationErrorThenThrow("g.é", Error.INVALID_SEGMENT, "é");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_04() {
    assertValidationErrorThenThrow("g.", Error.ILLEGAL_ENDING, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_04b() {
    assertValidationErrorThenThrow("g.foo.", Error.ILLEGAL_ENDING, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_05() {
    assertValidationErrorThenThrow("g..bar", Error.INVALID_SEGMENT, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_06() {
    assertValidationErrorThenThrow("g.foo.@.baz", Error.INVALID_SEGMENT, "@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_07() {
    assertValidationErrorThenThrow("g.foo.bar.a@1", Error.INVALID_SEGMENT, "a@1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_08() {
    assertValidationErrorThenThrow("g.foo.bar.baz ", Error.INVALID_SEGMENT, "baz ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_09() {
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

  private void assertValidationErrorThenThrow(final String actualAddressString,
      final Error expectedError, final Object... errorParams) {
    try {
      InterledgerAddress.builder().value(actualAddressString).build();
      fail("Should have thrown an IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage(), is(String.format(expectedError.getMessageFormat(), errorParams)));
      throw e;
    } catch (final Exception e) {
      fail("Should have thrown an IllegalArgumentException");
      throw e;
    }
  }


}
