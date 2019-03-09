package org.interledger.ildcp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link IldcpResponsePacketHandler}.
 */
public class IldcpResponsePacketHandlerTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private static final IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  private InterledgerResponsePacket fulfillPacket;
  private InterledgerResponsePacket rejectPacket;
  private InterledgerResponsePacket expiredPacket;

  @Before
  public void setup() {
    fulfillPacket = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();

    rejectPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message("rejected!")
        .build();

    expiredPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT)
        .message("Timed out!")
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void handleNullPacket() {
    new IldcpResponsePacketHandler() {

      @Override
      protected void handleIldcpResponsePacket(IldcpResponsePacket ildcpResponsePacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleIldcpErrorPacket(InterledgerRejectPacket ildcpErrorPacket) {
        throw new RuntimeException("Should not reject!");
      }

    }.handle(null);
    fail();
  }

  @Test
  public void handleFulfillPacket() {
    new IldcpResponsePacketHandler() {

      @Override
      protected void handleIldcpResponsePacket(IldcpResponsePacket ildcpResponsePacket) {
        assertThat(ildcpResponsePacket, is(fulfillPacket));
      }

      @Override
      protected void handleIldcpErrorPacket(InterledgerRejectPacket ildcpErrorPacket) {
        throw new RuntimeException("Should not reject!");
      }

    }.handle(fulfillPacket);
  }

  @Test
  public void handleRejectPacket() {
    new IldcpResponsePacketHandler() {

      @Override
      protected void handleIldcpResponsePacket(IldcpResponsePacket ildcpResponsePacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleIldcpErrorPacket(InterledgerRejectPacket ildcpErrorPacket) {
        assertThat(ildcpErrorPacket.getCode(), is(InterledgerErrorCode.T00_INTERNAL_ERROR));
      }

    }.handle(rejectPacket);
  }

  @Test
  public void handleExpiredPacket() {
    new IldcpResponsePacketHandler() {

      @Override
      protected void handleIldcpResponsePacket(IldcpResponsePacket ildcpResponsePacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleIldcpErrorPacket(InterledgerRejectPacket ildcpErrorPacket) {
        assertThat(ildcpErrorPacket.getCode(), is(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT));
      }
    }.handle(expiredPacket);
  }

}