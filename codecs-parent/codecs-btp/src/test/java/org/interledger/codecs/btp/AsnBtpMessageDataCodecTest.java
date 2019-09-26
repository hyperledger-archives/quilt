package org.interledger.codecs.btp;

import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.encoding.asn.framework.AsnObjectCodec;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class AsnBtpMessageDataCodecTest {

    private AsnBtpMessageDataCodec codec;

    private BtpMessage btpMessage;

    @Before
    public void setUp() {
        long REQUEST_ID = 123L;
        codec = new AsnBtpMessageDataCodec(REQUEST_ID);
        BtpSubProtocols btpSubProtocols = new BtpSubProtocols();

        btpSubProtocols.add(BtpSubProtocol.builder()
                .protocolName("TEST")
                .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
                .data("Test Data".getBytes(StandardCharsets.UTF_8))
                .build());

        btpMessage = BtpMessage.builder()
                        .requestId(REQUEST_ID)
                        .subProtocols(btpSubProtocols)
                        .build();
    }

    @Test
    public void encodeDecode() {
        codec.encode(btpMessage);

        assertThat(codec.getRequestId()).isEqualTo(btpMessage.getRequestId());
        assertThat(((BtpSubProtocols) codec.getValueAt(0)).get(0)).isEqualTo(btpMessage.getSubProtocol("TEST").get());

        final BtpMessage decodedMessage = codec.decode();

        assertThat(decodedMessage).isEqualTo(btpMessage);
        assertThat(decodedMessage.getSubProtocols().size()).isEqualTo(btpMessage.getSubProtocols().size());
        assertThat(decodedMessage.getType().getCode()).isEqualTo(btpMessage.getType().getCode());
        assertThat(decodedMessage.getSubProtocols().get(0)).isEqualTo(btpMessage.getSubProtocol("TEST").get());
    }
}
