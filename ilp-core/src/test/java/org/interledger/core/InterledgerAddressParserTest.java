package org.interledger.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit tests to test {@link InterledgerAddressParser}.
 */
public class InterledgerAddressParserTest {

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

  private InterledgerAddressParser addressParser;

  @Before
  public void before() {
    addressParser = new InterledgerAddressParser();
  }

  @Test
  public void testIsSchemePrefix() {
    assertThat(addressParser.isSchemePrefix("self"), is(false));
    assertThat(addressParser.isSchemePrefix("self."), is(true));
    assertThat(addressParser.isSchemePrefix("g."), is(true));
    assertThat(addressParser.isSchemePrefix("g.foo"), is(false));
    assertThat(addressParser.isSchemePrefix("g.foo."), is(false));
    assertThat(addressParser.isSchemePrefix("g.g."), is(false));
    assertThat(addressParser.isSchemePrefix("g.foo.bar"), is(false));
    assertThat(addressParser.isSchemePrefix(JUST_RIGHT), is(false));
  }

  @Test(expected = NullPointerException.class)
  public void testIsSchemePrefixWithNullAddress() {
    assertThat(addressParser.isSchemePrefix(null), is(false));
  }

  @Test
  public void testIsSchemePrefixWithEmptyAddress() {
    assertThat(addressParser.isSchemePrefix(""), is(false));
  }

  @Test
  public void testIsSchemePrefixWithInvalidAddresses() {
    assertThat(addressParser.isSchemePrefix("g"), is(false));
    assertThat(addressParser.isSchemePrefix("self"), is(false));
    assertThat(addressParser.isSchemePrefix("."), is(false));
    assertThat(addressParser.isSchemePrefix(".self"), is(false));
    assertThat(addressParser.isSchemePrefix(".foo"), is(false));
    assertThat(addressParser.isSchemePrefix(".@@@"), is(false));
    assertThat(addressParser.isSchemePrefix("gg."), is(false));
    assertThat(addressParser.isSchemePrefix(" g."), is(false));
    assertThat(addressParser.isSchemePrefix("g ."), is(false));
    assertThat(addressParser.isSchemePrefix("g. "), is(false));
    assertThat(addressParser.isSchemePrefix("g.\r\n"), is(false));
    assertThat(addressParser.isSchemePrefix("foo."), is(false));
    assertThat(addressParser.isSchemePrefix("@@@."), is(false));
    assertThat(addressParser.isSchemePrefix("g.foo"), is(false));
  }

  @Test
  public void testRequireAddressPrefix() {
    assertThat(addressParser.requireAddressPrefix(InterledgerAddress.of("g.")),
        is(InterledgerAddress.of("g.")));
    assertThat(addressParser.requireAddressPrefix(InterledgerAddress.of("g.foo.")),
        is(InterledgerAddress.of("g.foo.")));
  }

  @Test(expected = NullPointerException.class)
  public void testRequireAddressPrefixWithNullAddress() {
    try {
      addressParser.requireAddressPrefix(null);
    } catch (final NullPointerException e) {
      assertThat(e.getMessage(), is("InterledgerAddress must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void testRequireAddressPrefixWithNullErrorMessage() {
      addressParser.requireAddressPrefix(InterledgerAddress.of("g."), null);
  }

  @Test(expected = NullPointerException.class)
  public void testRequireAddressPrefixWithNullAddressAndErrorMessage() {
    try {
      addressParser.requireAddressPrefix(null, "An address prefix is mandatory");
    } catch (final NullPointerException e) {
      assertThat(e.getMessage(), is("An address prefix is mandatory"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRequireAddressPrefixWithNoAddressPrefix() {
    try {
      addressParser.requireAddressPrefix(InterledgerAddress.of("g.foo.bar"));
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage(), is("InterledgerAddress 'g.foo.bar' must be an Address Prefix"
          + " ending with a dot (.)"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRequireAddressPrefixWithNoAddressPrefixAndErrorMessage() {
    try {
      addressParser.requireAddressPrefix(InterledgerAddress.of("g.foo.bar"), "An address prefix"
          + " is mandatory");
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage(), is("An address prefix is mandatory"));
      throw e;
    }
  }

  @Test
  public void testRequireNotAddressPrefix() {
    assertThat(addressParser.requireNotAddressPrefix(InterledgerAddress.of("g.foo.bar")),
        is(InterledgerAddress.of("g.foo.bar")));
    assertThat(addressParser.requireNotAddressPrefix(InterledgerAddress.of("g.foo.bar.baz")),
        is(InterledgerAddress.of("g.foo.bar.baz")));
  }

  @Test(expected = NullPointerException.class)
  public void testRequireNotAddressPrefixWithNullAddress() {
    try {
      addressParser.requireNotAddressPrefix(null);
    } catch (final NullPointerException e) {
      assertThat(e.getMessage(), is("InterledgerAddress must not be null!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void testRequireNotAddressPrefixWithNullErrorMessage() {
      addressParser.requireNotAddressPrefix(InterledgerAddress.of("g.foo.bar"), null);
  }

  @Test(expected = NullPointerException.class)
  public void testRequireNotAddressPrefixWithNullAddressAndErrorMessage() {
    try {
      addressParser.requireNotAddressPrefix(null, "A destination address is mandatory");
    } catch (final NullPointerException e) {
      assertThat(e.getMessage(), is("A destination address is mandatory"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRequireNotAddressPrefixWithNoAddressPrefix() {
    try {
      addressParser.requireNotAddressPrefix(InterledgerAddress.of("g.foo."));
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage(), is("InterledgerAddress 'g.foo.' must NOT be an Address"
          + " Prefix ending with a dot (.)"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRequireNotAddressPrefixWithNoAddressPrefixAndErrorMessage() {
    try {
      addressParser.requireNotAddressPrefix(InterledgerAddress.of("g.foo."), "A destination"
          + " address is mandatory");
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage(), is("A destination address is mandatory"));
      throw e;
    }
  }

  @Test
  public void testValidate() {
    addressParser.validate("self.");
    addressParser.validate("g.");
    addressParser.validate("g.foo.");
    addressParser.validate("g.g.");
    addressParser.validate("g.foo.bar");
    addressParser.validate("g.foo.bar.");
    addressParser.validate("g.foo.bar.baz");
    addressParser.validate(JUST_RIGHT);
  }

  @Test(expected = NullPointerException.class)
  public void testValidateWithNullAddress() {
    addressParser.validate(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithEmptyAddress() {
    assertValidationErrorThenThrow("", InterledgerAddressParser.Error.MISSING_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_01() {
    assertValidationErrorThenThrow("g", InterledgerAddressParser.Error.MISSING_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_02() {
    assertValidationErrorThenThrow("self", InterledgerAddressParser.Error.MISSING_SCHEME_PREFIX);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_03() {
    assertValidationErrorThenThrow(".", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_04() {
    assertValidationErrorThenThrow(".foo", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_05() {
    assertValidationErrorThenThrow("gg.", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX, "gg");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_06() {
    assertValidationErrorThenThrow(" g.", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX, " g");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_07() {
    assertValidationErrorThenThrow("g .", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX, "g ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSchemePrefix_08() {
    assertValidationErrorThenThrow("@@@.", InterledgerAddressParser.Error.INVALID_SCHEME_PREFIX, "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_01() {
    assertValidationErrorThenThrow("g.@@@", InterledgerAddressParser.Error.INVALID_SEGMENT, "@@@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_02() {
    assertValidationErrorThenThrow("g.\u0000", InterledgerAddressParser.Error.INVALID_SEGMENT, "\u0000");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_03() {
    assertValidationErrorThenThrow("g.\u00E9", InterledgerAddressParser.Error.INVALID_SEGMENT, "\u00E9");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_04() {
    assertValidationErrorThenThrow("g..", InterledgerAddressParser.Error.INVALID_SEGMENT, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_05() {
    assertValidationErrorThenThrow("g..bar", InterledgerAddressParser.Error.INVALID_SEGMENT, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_06() {
    assertValidationErrorThenThrow("g.foo.@.baz", InterledgerAddressParser.Error.INVALID_SEGMENT, "@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_07() {
    assertValidationErrorThenThrow("g.foo.bar.a@1", InterledgerAddressParser.Error.INVALID_SEGMENT, "a@1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_08() {
    assertValidationErrorThenThrow("g.foo.bar.baz ", InterledgerAddressParser.Error.INVALID_SEGMENT,
        "baz ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithInvalidSegment_09() {
    assertValidationErrorThenThrow("g.foo.bar.baz\r\n", InterledgerAddressParser.Error.INVALID_SEGMENT,
        "baz\r\n");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithSegmentsUnderflow() {
    assertValidationErrorThenThrow("g.foo", InterledgerAddressParser.Error.SEGMENTS_UNDERFLOW);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateWithLengthOverflow() {
    assertValidationErrorThenThrow(TOO_LONG, InterledgerAddressParser.Error.ADDRESS_OVERFLOW);
  }

  private void assertValidationErrorThenThrow(final String actualAddressString,
      final InterledgerAddressParser.Error expectedError, final Object... errorParams) {
    try {
      addressParser.validate(actualAddressString);
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