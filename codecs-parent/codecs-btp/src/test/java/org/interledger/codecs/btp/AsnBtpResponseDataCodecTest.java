package org.interledger.codecs.btp;

import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class AsnBtpResponseDataCodecTest {

    private AsnBtpResponseDataCodec codec;

    private BtpResponse btpResponse;

    @Before
    public void setUp() {
        long REQUEST_ID = 123L;
        codec = new AsnBtpResponseDataCodec(REQUEST_ID);

        BtpSubProtocols btpSubProtocols = new BtpSubProtocols();

        btpSubProtocols.add(BtpSubProtocol.builder()
                .protocolName("TEST")
                .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
                .data("Test Data".getBytes(StandardCharsets.UTF_8))
                .build());

        btpResponse = BtpResponse.builder()
                        .requestId(REQUEST_ID)
                        .subProtocols(btpSubProtocols)
                        .build();
    }

    @Test
    public void encodeDecode() {
        codec.encode(btpResponse);

        assertThat(codec.getRequestId()).isEqualTo(btpResponse.getRequestId());
        assertThat(((BtpSubProtocols) codec.getValueAt(0)).size()).isEqualTo(btpResponse.getType().getCode());
        assertThat(((BtpSubProtocols) codec.getValueAt(0)).getPrimarySubProtocol()).isEqualTo(btpResponse.getSubProtocol("TEST").get());

        final BtpResponse decodedBtpResponse = codec.decode();

        assertThat(decodedBtpResponse).isEqualTo(btpResponse);
    }
}
