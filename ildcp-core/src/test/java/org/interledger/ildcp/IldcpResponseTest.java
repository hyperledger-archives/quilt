package org.interledger.ildcp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.core.InterledgerAddress;

import org.junit.Test;

/**
 * Unit tests for {@link IldcpResponse}.
 */
public class IldcpResponseTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  @Test(expected = IllegalStateException.class)
  public void testBuilderWhenEmpty() {
    try {
      IldcpResponse.builder().build();
    } catch (IllegalStateException e) {
      assertThat(
          e.getMessage(),
          is("Cannot build IldcpResponse, some of required attributes are not set [clientAddress, assetCode, assetScale]")
      );
      throw e;
    }
  }

  @Test
  public void testBuilder() {
    final IldcpResponse response = IldcpResponse.builder()
        .clientAddress(FOO_ADDRESS)
        .assetScale((short) 9)
        .assetCode(BTC)
        .build();

    assertThat(response.getClientAddress(), is(FOO_ADDRESS));
    assertThat(response.getAssetScale(), is((short) 9));
    assertThat(response.getAssetCode(), is(BTC));
  }
}