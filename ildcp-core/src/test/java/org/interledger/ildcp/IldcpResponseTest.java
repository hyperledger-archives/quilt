package org.interledger.ildcp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;

import org.junit.Test;

/**
 * Unit tests for {@link IldcpResponse}.
 */
public class IldcpResponseTest {

  @Test
  public void testBuilder() {
    final IldcpResponse actual = IldcpResponse.builder()
        .assetCode("BTC")
        .assetScale(9)
        .clientAddress(InterledgerAddress.of("example.foo"))
        .build();
    assertThat(actual.getAssetCode(), is("BTC"));
    assertThat(actual.getAssetScale(), is(9));
    assertThat(actual.getClientAddress(), is(InterledgerAddress.of("example.foo")));
  }

  @Test(expected = IllegalStateException.class)
  public void testEmptyBuilder() {
    try {
      IldcpResponse.builder().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(
          e.getMessage(),
          is("Cannot build IldcpResponse, some of required attributes are not set [clientAddress, assetCode, assetScale]")
      );
      throw e;
    }
  }

  @Test
  public void testEqualsHashcode() {

    final IldcpResponse first = IldcpResponse.builder()
        .assetCode("BTC")
        .assetScale(9)
        .clientAddress(InterledgerAddress.of("example.foo"))
        .build();
    final IldcpResponse second = IldcpResponse.builder()
        .assetCode("BTC")
        .assetScale(9)
        .clientAddress(InterledgerAddress.of("example.foo"))
        .build();

    final IldcpResponse third = IldcpResponse.builder()
        .assetCode("BTC")
        .assetScale(2)
        .clientAddress(InterledgerAddress.of("example.foo"))
        .build();

    assertThat(first.equals(second), is(true));
    assertThat(second.equals(first), is(true));

    assertThat(
        IldcpResponse.builder()
            .assetCode("XRP")
            .assetScale(9)
            .clientAddress(InterledgerAddress.of("example.foo"))
            .build(),
        is(not(first))
    );

    assertThat(
        IldcpResponse.builder()
            .assetCode("BTC")
            .assetScale(8)
            .clientAddress(InterledgerAddress.of("example.foo"))
            .build(),
        is(not(first))
    );

    assertThat(
        IldcpResponse.builder()
            .assetCode("BTC")
            .assetScale(9)
            .clientAddress(InterledgerAddress.of("example.foo.bar"))
            .build(),
        is(not(first))
    );

    assertThat(first.hashCode(), is(second.hashCode()));
    assertThat(second.hashCode(), is(first.hashCode()));
    assertThat(IldcpResponse.builder()
        .assetCode("BTC")
        .assetScale(9)
        .clientAddress(InterledgerAddress.of("example.foo.bar"))
        .build().hashCode(), is(not(first.hashCode())));
  }

  @Test
  public void testToString() {
    final IldcpResponse first = IldcpResponse.builder()
        .assetCode("BTC")
        .assetScale(9)
        .clientAddress(InterledgerAddress.of("example.foo.bar"))
        .build();

    assertThat(
        first.toString(),
        is("IldcpResponse{clientAddress=InterledgerAddress{value=example.foo.bar}, assetCode=BTC, assetScale=9}")
    );
  }
}