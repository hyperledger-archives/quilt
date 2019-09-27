package org.interledger.ildcp;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class IldcpResponsePacketMapperTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private static final IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  private InterledgerResponsePacket fulfillPacket;
  private InterledgerResponsePacket rejectPacket;
  private InterledgerResponsePacket shampooPacket;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setup() {
    fulfillPacket = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();

    rejectPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message("rejected!")
        .build();

    shampooPacket = InterledgerShampooPacket.builder().build();
  }

  @Test
  public void mapFulfill() {
    ResponseWrapper wrapper = new ResponseWrapperMapper().map(fulfillPacket);
    assertThat(wrapper.get()).isEqualTo(fulfillPacket);
  }

  @Test
  public void mapReject() {
    ResponseWrapper wrapper = new ResponseWrapperMapper().map(rejectPacket);
    assertThat(wrapper.get()).isEqualTo(rejectPacket);
  }

  @Test
  public void cannotMap() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Unsupported IldcpResponsePacket Type: " +
        "class org.interledger.ildcp.InterledgerShampooPacketBuilder$ImmutableInterledgerShampooPacket");
    new ResponseWrapperMapper().map(shampooPacket);
  }

  public class ResponseWrapper {

    private final InterledgerResponsePacket responsePacket;

    public ResponseWrapper(InterledgerResponsePacket responsePacket) {
      this.responsePacket = responsePacket;
    }

    public InterledgerResponsePacket get() {
      return responsePacket;
    }

  }

  public class ResponseWrapperMapper extends IldcpResponsePacketMapper<ResponseWrapper> {

    @Override
    protected ResponseWrapper mapFulfillPacket(IldcpResponsePacket ildcpResponsePacket) {
      return new ResponseWrapper(ildcpResponsePacket);
    }

    @Override
    protected ResponseWrapper mapRejectPacket(InterledgerRejectPacket ildcpRejectPacket) {
      return new ResponseWrapper(ildcpRejectPacket);
    }
  }
}
