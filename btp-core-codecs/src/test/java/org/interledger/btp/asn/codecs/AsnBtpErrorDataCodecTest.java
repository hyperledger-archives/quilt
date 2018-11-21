package org.interledger.btp.asn.codecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.btp.BtpError;
import org.interledger.btp.BtpErrorCode;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

/**
 * Unit tests for {@link AsnBtpErrorDataCodec}.
 */
public class AsnBtpErrorDataCodecTest {

  private AsnBtpErrorCodec codec;

  private BtpError btpError;

  @Before
  public void setUp() {
    codec = new AsnBtpErrorCodec();
    btpError = BtpError.builder()
        .errorCode(BtpErrorCode.F00_NotAcceptedError)
        .triggeredAt(Instant.now())
        .requestId(123L).build();
  }

  @Test
  public void decode() {
    final AsnUint8Codec uint8Codec = new AsnUint8Codec();
    uint8Codec.encode((short) 2);

    codec.setValueAt(0, (short) 2);
    codec.setValueAt(1, 123L);
    codec.setValueAt(2, btpError);

    final BtpError decodedBtpError = codec.decode();
    assertThat(decodedBtpError, is(btpError));
  }

  @Test
  public void encode() {
    codec.encode(btpError);
    assertThat(codec.getValueAt(0), is(btpError.getType().getCode()));
    assertThat(codec.getValueAt(1), is(btpError.getRequestId()));
    assertThat(codec.getValueAt(2), is(btpError));

    final BtpError decodedBtpError = codec.decode();
    assertThat(decodedBtpError, is(btpError));
  }
}