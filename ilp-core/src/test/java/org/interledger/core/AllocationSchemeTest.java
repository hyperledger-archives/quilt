package org.interledger.core;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.Error.ILLEGAL_ENDING;

import org.interledger.core.InterledgerAddress.AllocationScheme;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AllocationSchemeTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void ofWithInvalidScheme() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'foo' AllocationScheme is invalid!");
    AllocationScheme.of("foo");
  }

  @Test
  public void ofWithInvalidButSimilarScheme() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'local1' AllocationScheme is invalid!");
    AllocationScheme.of("local1");
  }

  @Test
  public void of() {
    assertThat(AllocationScheme.of("g")).isEqualTo(AllocationScheme.GLOBAL);
    assertThat(AllocationScheme.of("private")).isEqualTo(AllocationScheme.PRIVATE);
    assertThat(AllocationScheme.of("example")).isEqualTo(AllocationScheme.EXAMPLE);
    assertThat(AllocationScheme.of("peer")).isEqualTo(AllocationScheme.PEER);
    assertThat(AllocationScheme.of("self")).isEqualTo(AllocationScheme.SELF);
    assertThat(AllocationScheme.of("test")).isEqualTo(AllocationScheme.TEST);
    assertThat(AllocationScheme.of("test1")).isEqualTo(AllocationScheme.TEST1);
    assertThat(AllocationScheme.of("test2")).isEqualTo(AllocationScheme.TEST2);
    assertThat(AllocationScheme.of("test3")).isEqualTo(AllocationScheme.TEST3);
    assertThat(AllocationScheme.of("local")).isEqualTo(AllocationScheme.LOCAL);
  }

  @Test
  public void getValue() {
    assertThat(AllocationScheme.of("g").getValue()).isEqualTo("g");
    assertThat(AllocationScheme.of("private").getValue()).isEqualTo("private");
    assertThat(AllocationScheme.of("example").getValue()).isEqualTo("example");
    assertThat(AllocationScheme.of("peer").getValue()).isEqualTo("peer");
    assertThat(AllocationScheme.of("self").getValue()).isEqualTo("self");
    assertThat(AllocationScheme.of("test").getValue()).isEqualTo("test");
    assertThat(AllocationScheme.of("test1").getValue()).isEqualTo("test1");
    assertThat(AllocationScheme.of("test2").getValue()).isEqualTo("test2");
    assertThat(AllocationScheme.of("test3").getValue()).isEqualTo("test3");
    assertThat(AllocationScheme.of("local").getValue()).isEqualTo("local");
  }

  @Test
  public void testInterledgerAddressCreationWithCorrectAllocationScheme() {
    InterledgerAddress globalAddress = AllocationScheme.GLOBAL.with("bob.baz");
    Assertions.assertThat(globalAddress.getAllocationScheme()).isEqualTo(AllocationScheme.GLOBAL);
    Assertions.assertThat(globalAddress.getValue()).isEqualTo("g.bob.baz");

    InterledgerAddress exampleAddress = AllocationScheme.EXAMPLE.with("bob.baz");
    Assertions.assertThat(exampleAddress.getAllocationScheme()).isEqualTo(AllocationScheme.EXAMPLE);
    Assertions.assertThat(exampleAddress.getValue()).isEqualTo("example.bob.baz");

    InterledgerAddress privateAddress = AllocationScheme.PRIVATE.with("bob.baz");
    Assertions.assertThat(privateAddress.getAllocationScheme()).isEqualTo(AllocationScheme.PRIVATE);
    Assertions.assertThat(privateAddress.getValue()).isEqualTo("private.bob.baz");

    InterledgerAddress peerAddress = AllocationScheme.PEER.with("bob.baz");
    Assertions.assertThat(peerAddress.getAllocationScheme()).isEqualTo(AllocationScheme.PEER);
    Assertions.assertThat(peerAddress.getValue()).isEqualTo("peer.bob.baz");

    InterledgerAddress selfAddress = AllocationScheme.SELF.with("bob.baz");
    Assertions.assertThat(selfAddress.getAllocationScheme()).isEqualTo(AllocationScheme.SELF);
    Assertions.assertThat(selfAddress.getValue()).isEqualTo("self.bob.baz");

    InterledgerAddress testAddress = AllocationScheme.TEST.with("bob.baz");
    Assertions.assertThat(testAddress.getAllocationScheme()).isEqualTo(AllocationScheme.TEST);
    Assertions.assertThat(testAddress.getValue()).isEqualTo("test.bob.baz");

    InterledgerAddress test1Address = AllocationScheme.TEST1.with("bob.baz");
    Assertions.assertThat(test1Address.getAllocationScheme()).isEqualTo(AllocationScheme.TEST1);
    Assertions.assertThat(test1Address.getValue()).isEqualTo("test1.bob.baz");

    InterledgerAddress test2Address = AllocationScheme.TEST2.with("bob.baz");
    Assertions.assertThat(test2Address.getAllocationScheme()).isEqualTo(AllocationScheme.TEST2);
    Assertions.assertThat(test2Address.getValue()).isEqualTo("test2.bob.baz");

    InterledgerAddress test3Address = AllocationScheme.TEST3.with("bob.baz");
    Assertions.assertThat(test3Address.getAllocationScheme()).isEqualTo(AllocationScheme.TEST3);
    Assertions.assertThat(test3Address.getValue()).isEqualTo("test3.bob.baz");

    InterledgerAddress localAddress = AllocationScheme.LOCAL.with("bob.baz");
    Assertions.assertThat(localAddress.getAllocationScheme()).isEqualTo(AllocationScheme.LOCAL);
    Assertions.assertThat(localAddress.getValue()).isEqualTo("local.bob.baz");
  }

  @Test(expected = NullPointerException.class)
  public void testInterledgerAddressCreationWithNull() {
    try {
      AllocationScheme.GLOBAL.with(null);
      fail("should have failed and the error been caught");
    } catch (NullPointerException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo("value must not be null!");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInterledgerAddressCreationWithEmpty() {
    try {
      AllocationScheme.GLOBAL.with("");
      fail("should have failed and the error been caught");
    } catch (IllegalArgumentException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo(ILLEGAL_ENDING.getMessageFormat());
      throw e;
    }
  }

  @Test
  public void testAllocationScheme() {
    Assertions.assertThat(InterledgerAddress.of("g.foo.bob").getAllocationScheme())
        .isEqualTo(AllocationScheme.builder().value("g").build());
  }
}
