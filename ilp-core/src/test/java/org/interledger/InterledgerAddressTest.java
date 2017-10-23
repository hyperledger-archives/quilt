package org.interledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * JUnit tests to test {@link InterledgerAddress}.
 */
public class InterledgerAddressTest {

  private static final boolean IS_LEDGER_PREFIX = true;
  private static final boolean IS_NOT_LEDGER_PREFIX = false;
  private static final String EXPECTED_ERROR_MESSAGE =
      "Invalid characters in address: ['%s']. Reference Interledger RFC-15 for proper format.";

  @Test
  public void testGetValue() throws Exception {
    assertThat(InterledgerAddress.of("g.foo.bob").getValue(), is("g.foo.bob"));
  }

  @Test
  public void testIsLedgerPrefix() throws Exception {
    assertThat(InterledgerAddress.of("g.foo.bob").isLedgerPrefix(), is(IS_NOT_LEDGER_PREFIX));
    assertThat(InterledgerAddress.of("g.foo.bob.").isLedgerPrefix(), is(IS_LEDGER_PREFIX));
  }

  @Test
  public void testStartsWithString() throws Exception {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith("g"), is(true));
    assertThat(address.startsWith("g."), is(true));
    assertThat(address.startsWith("g.foo"), is(true));
    assertThat(address.startsWith("g.foo."), is(true));
    assertThat(address.startsWith("g.foo.bob"), is(true));
    assertThat(address.startsWith("g.foo.bob."), is(false));
    assertThat(address.startsWith("test.foo.bob"), is(false));
  }

  @Test
  public void testStartsWithInterledgerAddress() throws Exception {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith(InterledgerAddress.of("g.")), is(true));
    assertThat(address.startsWith(InterledgerAddress.of("g.foo")), is(true));
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.")), is(true));
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.bob")), is(true));
    assertThat(address.startsWith(InterledgerAddress.of("g.foo.bob.")), is(false));
    assertThat(address.startsWith(InterledgerAddress.of("test1.foo.bob")), is(false));
  }

  @Test(expected = NullPointerException.class)
  public void testAddressWithNull() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo.");
    try {
      addressPrefix.with(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("addressSegment must not be null!"));
      throw e;
    }
  }

  @Test
  public void testAddressWithEmpty() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo.");
    assertThat(addressPrefix.with("").getValue(), is("g.foo."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddressWithBlank() {
    final String value = "g.foo.  ";
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo.");
    try {
      addressPrefix.with("  ");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(String.format(EXPECTED_ERROR_MESSAGE, value)));
      throw e;
    }
  }

  /**
   * Validates adding an address prefix (as an InterledgerAddress) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithAddressPrefix() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo");
    final String additionalDestinationAddress = "bob.";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob."));
  }

  /**
   * Validates adding a destination address (as an InterledgerAddress) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithDestinationAddress() {
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo.");
    final String additionalDestinationAddress = "bob";
    assertThat(addressPrefix.with(additionalDestinationAddress).getValue(), is("g.foo.bob"));
  }

  /**
   * Validates adding a destination address (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testDestinationAddressWithDestinationAddress() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo");
    final String additionalDestinationAddress = "bob";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob"));
  }

  /**
   * Validates adding an address prefix  (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testDestinationAddressWithAddressPrefix() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo");
    final String additionalDestinationAddress = "bob.";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob."));
  }

  @Test
  public void testGetPrefixFromShortPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.");
    assertThat(address.getPrefix().getValue(), is("g."));
  }

  @Test
  public void testGetPrefixFromLongPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo.");
    assertThat(address.getPrefix().getValue(), is("g.alpha.beta.charlie.delta.echo."));
  }

  @Test
  public void testGetPrefixFromPrefix() {
    final InterledgerAddress address = InterledgerAddress.of("g.example.");
    assertThat(address.getPrefix().getValue(), is("g.example."));
  }

  @Test
  public void testGetPrefixFromAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.example.bob");
    assertThat(address.getPrefix().getValue(), is("g.example."));
  }

  @Test
  public void testGetPrefixFromShortAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.bob");
    assertThat(address.getPrefix().getValue(), is("g."));
  }

  @Test
  public void testGetPrefixFromLongAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().getValue(), is("g.alpha.beta.charlie.delta."));
  }

  @Test
  public void testAddressEqualsHashcode() {
    final InterledgerAddress addressPrefix1 = InterledgerAddress.of("g.foo");
    final InterledgerAddress addressPrefix2 = InterledgerAddress.of("g.foo");
    final InterledgerAddress addressPrefix3 = InterledgerAddress.of("g.foo.");

    assertThat(addressPrefix1.hashCode() == addressPrefix2.hashCode(), is(true));
    assertThat(addressPrefix1.equals(addressPrefix2), is(true));
    assertThat(addressPrefix2.equals(addressPrefix1), is(true));
    assertThat(addressPrefix1.toString().equals(addressPrefix2.toString()), is(true));

    assertThat(addressPrefix1.hashCode() == addressPrefix3.hashCode(), is(false));
    assertThat(addressPrefix1.equals(addressPrefix3), is(false));
    assertThat(addressPrefix3.equals(addressPrefix1), is(false));
    assertThat(addressPrefix1.toString().equals(addressPrefix3.toString()), is(false));
  }

  @Test
  public void testToString() throws Exception {
    assertThat(InterledgerAddress.of("g.foo.bob").toString(), is("g.foo.bob"));
  }

  /**
   * Assert that a non-ledger prefix fails the
   * {@link InterledgerAddress#requireLedgerPrefix(InterledgerAddress)} check.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRequireLedgerPrefixNotLedgerPrefix() {
    try {
      InterledgerAddress.requireLedgerPrefix(InterledgerAddress.of("example.bar"));
      fail("Should have thrown an IllegalArgumentException!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
          is("InterledgerAddress 'example.bar' must be a Ledger Prefix ending with a dot (.)"));
      throw e;
    }
  }

  /**
   * Assert that a ledger prefix passes the
   * {@link InterledgerAddress#requireLedgerPrefix(InterledgerAddress)} check.
   */
  @Test
  public void testRequireLedgerPrefixIsLedgerPrefix() {
    final InterledgerAddress controlPrefix = InterledgerAddress.of("example.bar.");
    final InterledgerAddress checkedLedgerPrefix = InterledgerAddress
        .requireLedgerPrefix(controlPrefix);

    assertThat(checkedLedgerPrefix, is(controlPrefix));
  }

  /**
   * Assert that a non-ledger prefix passes the
   * {@link InterledgerAddress#requireNotLedgerPrefix(InterledgerAddress)} check.
   */
  @Test
  public void testRequireNotLedgerPrefixNotLedgerPrefix() {
    final InterledgerAddress controlPrefix = InterledgerAddress.of("example.bar");
    final InterledgerAddress checkedLedgerPrefix = InterledgerAddress
        .requireNotLedgerPrefix(controlPrefix);

    assertThat(checkedLedgerPrefix, is(controlPrefix));
  }

  /**
   * Assert that a ledger prefix fails the
   * {@link InterledgerAddress#requireNotLedgerPrefix(InterledgerAddress)} check.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRequireNotLedgerPrefixIsLedgerPrefix() {
    try {
      InterledgerAddress.requireNotLedgerPrefix(InterledgerAddress.of("example.bar."));
      fail("Should have thrown an IllegalArgumentException!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(
          "InterledgerAddress 'example.bar.' must NOT be a Ledger Prefix ending with a dot (.)")
      );
      throw e;
    }
  }
}