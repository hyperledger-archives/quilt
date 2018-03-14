package org.interledger.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;

import org.junit.Test;

import java.net.URI;

/**
 * JUnit tests to test {@link InterledgerAddress}.
 */
public class InterledgerAddressTest {


  private static final boolean IS_LEDGER_PREFIX = true;
  private static final boolean IS_NOT_LEDGER_PREFIX = false;
  private static final String EXPECTED_ERROR_MESSAGE = "Address is invalid";

  @Test
  public void testGetValue() {
    assertThat(InterledgerAddress.of("g.foo.bob").getValue(), is("g.foo.bob"));
  }

  @Test
  public void testIsLedgerPrefix() {
    assertThat(InterledgerAddress.of("g.foo.bob").isLedgerPrefix(), is(IS_NOT_LEDGER_PREFIX));
    assertThat(InterledgerAddress.of("g.foo.bob.").isLedgerPrefix(), is(IS_LEDGER_PREFIX));
    assertThat(InterledgerAddress.of("g.").isLedgerPrefix(), is(IS_LEDGER_PREFIX));
    assertThat(InterledgerAddress.of("self.").isLedgerPrefix(), is(IS_LEDGER_PREFIX));
  }

  @Test
  public void testStartsWithString() {
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
  public void testStartsWithInterledgerAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.foo.bob");
    assertThat(address.startsWith(InterledgerAddress.of("g.")), is(true));
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
    final InterledgerAddress addressPrefix = InterledgerAddress.of("g.foo.");
    try {
      addressPrefix.with("  ");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(EXPECTED_ERROR_MESSAGE));
      throw e;
    }
  }

  /**
   * Validates adding an address prefix (as an InterledgerAddress) to an address prefix.
   */
  @Test
  public void testAddressPrefixWithAddressPrefix() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz.";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob.boz."));
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
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob.boz"));
  }

  /**
   * Validates adding an address prefix  (as an InterledgerAddress) to a destination address.
   */
  @Test
  public void testDestinationAddressWithAddressPrefix() {
    final InterledgerAddress destinationAddress = InterledgerAddress.of("g.foo.bob");
    final String additionalDestinationAddress = "boz.";
    assertThat(destinationAddress.with(additionalDestinationAddress).getValue(), is("g.foo.bob.boz."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDestinationAddressWithoutEnoughSegments() {
    try {
      InterledgerAddress.of("g.foo");
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage(), is(EXPECTED_ERROR_MESSAGE));
      throw e;
    }
  }

  @Test
  public void testGetPrefixRoot() {
    assertThat(InterledgerAddress.of("g.").getPrefix().getValue(), is("g."));
    assertThat(InterledgerAddress.of("self.").getPrefix().getValue(), is("self."));
    assertThat(InterledgerAddress.of("test.").getPrefix().getValue(), is("test."));
    assertThat(InterledgerAddress.of("test1.").getPrefix().getValue(), is("test1."));
    assertThat(InterledgerAddress.of("test2.").getPrefix().getValue(), is("test2."));
    assertThat(InterledgerAddress.of("test3.").getPrefix().getValue(), is("test3."));
  }

  @Test
  public void testGetPrefixFromShortPrefix() {
    assertThat(InterledgerAddress.of("g.1.").getPrefix().getValue(), is("g.1."));
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
    assertThat(InterledgerAddress.of("g.bob.foo").getPrefix().getValue(), is("g.bob."));
    assertThat(InterledgerAddress.of("g.b.f").getPrefix().getValue(), is("g.b."));
  }

  @Test
  public void testGetPrefixFromLongAddress() {
    final InterledgerAddress address = InterledgerAddress.of("g.alpha.beta.charlie.delta.echo");
    assertThat(address.getPrefix().getValue(), is("g.alpha.beta.charlie.delta."));
  }

  @Test
  public void testGetParentPrefix() {
    assertThat(InterledgerAddress.of("g.bob.").getParentPrefix().get().getValue(), is("g."));
    assertThat(InterledgerAddress.of("g.bob.foo").getParentPrefix().get().getValue(), is("g.bob."));
    assertThat(
        InterledgerAddress.of("g.bob.foo.").getParentPrefix().get().getValue(), is("g.bob.")
    );
    assertThat(InterledgerAddress.of("g.").getParentPrefix().isPresent(), is(false));
    assertThat(InterledgerAddress.of("private.").getParentPrefix().isPresent(), is(false));
    assertThat(InterledgerAddress.of("example.").getParentPrefix().isPresent(), is(false));
    assertThat(InterledgerAddress.of("peer.").getParentPrefix().isPresent(), is(false));
    assertThat(InterledgerAddress.of("self.").getParentPrefix().isPresent(), is(false));
    assertThat(InterledgerAddress.of("test.").getParentPrefix().isPresent(), is(false));
    assertThat(InterledgerAddress.of("test1.").getParentPrefix().isPresent(), is(false));
    assertThat(InterledgerAddress.of("test2.").getParentPrefix().isPresent(), is(false));
    assertThat(InterledgerAddress.of("test3.").getParentPrefix().isPresent(), is(false));
  }

  @Test
  public void testHasParentPrefix() {
    assertThat(InterledgerAddress.of("g.bob.").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("g.bob.foo").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("g.bob.foo.").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("self.bob.").hasParentPrefix(), is(true));

    assertThat(InterledgerAddress.of("g.1.1").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("private.1.1").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("example.1.1").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("peer.1.1").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("self.1.1").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("test.1.1").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("test1.1.1").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("test2.1.1").hasParentPrefix(), is(true));
    assertThat(InterledgerAddress.of("test3.1.1").hasParentPrefix(), is(true));

    assertThat(InterledgerAddress.of("g.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("private.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("example.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("peer.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("self.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("test.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("test1.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("test2.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("test3.").hasParentPrefix(), is(false));
  }

  @Test
  public void testIsRootPrefix() {
    assertThat(InterledgerAddress.of("g.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("private.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("example.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("peer.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("self.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("test.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("test1.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("test2.").hasParentPrefix(), is(false));
    assertThat(InterledgerAddress.of("test3.").hasParentPrefix(), is(false));
  }

  @Test
  public void testAddressEqualsHashcode() {
    final InterledgerAddress addressPrefix1 = InterledgerAddress.of("g.foo.bob");
    final InterledgerAddress addressPrefix2 = InterledgerAddress.of("g.foo.bob");
    final InterledgerAddress addressPrefix3 = InterledgerAddress.of("g.foo.bob.");

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
  public void testToString() {
    assertThat(InterledgerAddress.of("g.foo.bob").toString(),
        is("InterledgerAddress{value=g.foo.bob}"));
    assertThat(InterledgerAddress.of("g.foo.bob.").toString(),
        is("InterledgerAddress{value=g.foo.bob.}"));
    assertThat(InterledgerAddress.of("g.").toString(),
        is("InterledgerAddress{value=g.}"));
  }

  /**
   * Assert that {@link InterledgerAddress#requireAddressPrefix(InterledgerAddress)} fails with a
   * <tt>null</tt> address prefix.
   */
  @Test(expected = NullPointerException.class)
  public void testRequireLedgerPrefixWithNullPrefix() {
    try {
      InterledgerAddress.requireAddressPrefix(null);
      fail("Should have thrown an NullPointerException!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(),
          is("InterledgerAddress must not be null!"));
      throw e;
    }
  }

  /**
   * Assert that {@link InterledgerAddress#requireAddressPrefix(InterledgerAddress)} fails with a
   * <tt>null</tt> address prefix and a provided error message.
   */
  @Test(expected = NullPointerException.class)
  public void testRequireLedgerPrefixWithNullPrefixAndErrorMessage() {
    try {
      InterledgerAddress.requireAddressPrefix(null, "An address prefix is mandatory");
      fail("Should have thrown an NullPointerException!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(),
          is("An address prefix is mandatory"));
      throw e;
    }
  }

  /**
   * Assert that a non-address prefix fails the {@link InterledgerAddress#requireAddressPrefix(InterledgerAddress)}
   * check.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRequireAddressPrefixNotAddressPrefix() {
    try {
      InterledgerAddress.requireAddressPrefix(InterledgerAddress.of("example.bar.baz"));
      fail("Should have thrown an IllegalArgumentException!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
          is("InterledgerAddress 'example.bar.baz' must be an Address Prefix ending with a dot (.)"));
      throw e;
    }
  }

  /**
   * Assert that a non-address prefix fails the {@link InterledgerAddress#requireAddressPrefix(InterledgerAddress)}
   * check with the provided error message.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRequireAddressPrefixNotAddressPrefixWithErrorMessage() {
    try {
      InterledgerAddress.requireAddressPrefix(InterledgerAddress.of("example.bar.baz"),
          "An address prefix is mandatory");
      fail("Should have thrown an IllegalArgumentException!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("An address prefix is mandatory"));
      throw e;
    }
  }

  /**
   * Assert that a address prefix passes the {@link InterledgerAddress#requireAddressPrefix(InterledgerAddress)}
   * check.
   */
  @Test
  public void testRequireAddressPrefixIsAddressPrefix() {
    final InterledgerAddress controlPrefix = InterledgerAddress.of("example.bar.");
    final InterledgerAddress checkedAddressPrefix = InterledgerAddress
        .requireAddressPrefix(controlPrefix);

    assertThat(checkedAddressPrefix, is(controlPrefix));
  }

  /**
   * Assert that {@link InterledgerAddress#requireNotAddressPrefix(InterledgerAddress)} fails with a
   * <tt>null</tt> address prefix.
   */
  @Test(expected = NullPointerException.class)
  public void testRequireNotAddressPrefixWithNullPrefix() {
    try {
      InterledgerAddress.requireNotAddressPrefix(null);
      fail("Should have thrown an NullPointerException!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(),
          is("InterledgerAddress must not be null!"));
      throw e;
    }
  }

  /**
   * Assert that {@link InterledgerAddress#requireNotAddressPrefix(InterledgerAddress)} fails with a
   * <tt>null</tt> address prefix and a provided error message.
   */
  @Test(expected = NullPointerException.class)
  public void testRequireNotAddressPrefixWithNullPrefixAndErrorMessage() {
    try {
      InterledgerAddress.requireNotAddressPrefix(null, "A destination address is mandatory");
      fail("Should have thrown an NullPointerException!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(),
          is("A destination address is mandatory"));
      throw e;
    }
  }

  /**
   * Assert that a address prefix fails the {@link InterledgerAddress#requireNotAddressPrefix(InterledgerAddress)}
   * check.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRequireNotAddressPrefixIsAddressPrefix() {
    try {
      InterledgerAddress.requireNotAddressPrefix(InterledgerAddress.of("example.bar."));
      fail("Should have thrown an IllegalArgumentException!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(
          "InterledgerAddress 'example.bar.' must NOT be an Address Prefix ending with a dot (.)")
      );
      throw e;
    }
  }

  /**
   * Assert that a address prefix fails the {@link InterledgerAddress#requireNotAddressPrefix(InterledgerAddress)}
   * check with the provided error message.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRequireNotAddressPrefixIsAddressPrefixWithErrorMessage() {
    try {
      InterledgerAddress.requireNotAddressPrefix(InterledgerAddress.of("example.bar."),
          "A destination address is mandatory");
      fail("Should have thrown an IllegalArgumentException!");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("A destination address is mandatory"));
      throw e;
    }
  }

  /**
   * Assert that a non-address prefix passes the {@link InterledgerAddress#requireNotAddressPrefix(InterledgerAddress)}
   * check.
   */
  @Test
  public void testRequireNotAddressPrefixWhenNotAddressPrefix() {
    final InterledgerAddress controlPrefix = InterledgerAddress.of("example.bar.baz");
    final InterledgerAddress checkedAddressPrefix = InterledgerAddress
        .requireNotAddressPrefix(controlPrefix);

    assertThat(checkedAddressPrefix, is(controlPrefix));
  }

}