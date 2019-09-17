package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;
import org.interledger.core.InterledgerPacketType;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class StreamPacketTest {

  @Test
  public void version1() throws Exception {
    StreamPacket packet = StreamPacket.builder()
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        .sequence(UnsignedLong.ZERO)
        .prepareAmount(UnsignedLong.ZERO)
        .build();
    assertThat(packet.version()).isEqualTo((short) 1);
  }
}
