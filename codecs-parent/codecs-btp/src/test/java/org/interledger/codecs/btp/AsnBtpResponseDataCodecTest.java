package org.interledger.codecs.btp;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class AsnBtpResponseDataCodecTest {

  private AsnBtpResponseDataCodec codec;

  private BtpResponse btpResponse;

  @Before
  public void setUp() {
    long requestId = 123L;
    codec = new AsnBtpResponseDataCodec(requestId);

    BtpSubProtocols btpSubProtocols = new BtpSubProtocols();

    btpSubProtocols.add(BtpSubProtocol.builder()
        .protocolName("TEST")
        .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
        .data("Test Data".getBytes(StandardCharsets.UTF_8))
        .build());

    btpResponse = BtpResponse.builder()
        .requestId(requestId)
        .subProtocols(btpSubProtocols)
        .build();
  }

  @Test
  public void encodeDecode() {
    codec.encode(btpResponse);

    assertThat(codec.getRequestId()).isEqualTo(btpResponse.getRequestId());
    assertThat(((BtpSubProtocols) codec.getValueAt(0)).size()).isEqualTo(btpResponse.getType().getCode());
    assertThat(((BtpSubProtocols) codec.getValueAt(0)).getPrimarySubProtocol())
        .isEqualTo(btpResponse.getSubProtocol("TEST").get());

    final BtpResponse decodedBtpResponse = codec.decode();

    assertThat(decodedBtpResponse).isEqualTo(btpResponse);
  }
}
