package org.interledger.codecs.btp;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.btp.BtpTransfer;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class AsnBtpTransferDataCodecTest {

  private AsnBtpTransferDataCodec codec;

  private BtpTransfer btpTransfer;

  @Before
  public void setUp() {
    long requestId = 123L;
    codec = new AsnBtpTransferDataCodec(requestId);

    BtpSubProtocols btpSubProtocols = new BtpSubProtocols();

    btpSubProtocols.add(BtpSubProtocol.builder()
        .protocolName("TEST")
        .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
        .data("Test Data".getBytes(StandardCharsets.UTF_8))
        .build());

    btpTransfer = BtpTransfer.builder()
        .requestId(requestId)
        .amount(BigInteger.TEN)
        .subProtocols(btpSubProtocols)
        .build();
  }

  @Test
  public void encodeDecode() {
    codec.encode(btpTransfer);

    assertThat(codec.getRequestId()).isEqualTo(btpTransfer.getRequestId());
    assertThat((BigInteger) codec.getValueAt(0)).isEqualTo(btpTransfer.getAmount());
    assertThat((BtpSubProtocols) codec.getValueAt(1)).isEqualTo(btpTransfer.getSubProtocols());

    final BtpTransfer decodedBtpTransferMessage = codec.decode();

    assertThat(decodedBtpTransferMessage).isEqualTo(btpTransfer);
  }
}
