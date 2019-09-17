package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;
import org.interledger.core.InterledgerPacketType;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.spy;

public class StreamPacketTest {

  @Test
  public void version1() {
    StreamPacket packet = StreamPacket.builder()
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        .sequence(UnsignedLong.ZERO)
        .prepareAmount(UnsignedLong.ZERO)
        .build();
    assertThat(packet.version()).isEqualTo((short) 1);
    // make sure interface default method is exercised
    assertThat(spy(StreamPacket.class).version()).isEqualTo((short) 1);
  }
}
