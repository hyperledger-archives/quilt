package org.interledger.stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.spy;

import org.interledger.core.InterledgerPacketType;
import org.interledger.stream.frames.StreamFrame;

import java.util.List;
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

/**
 * Unit tests for {@link StreamPacket}.
 */
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
    final StreamPacket interfacePacket = new StreamPacket() {
      @Override
      public InterledgerPacketType interledgerPacketType() {
        return null;
      }

      @Override
      public UnsignedLong sequence() {
        return null;
      }

      @Override
      public UnsignedLong prepareAmount() {
        return null;
      }

      @Override
      public List<StreamFrame> frames() {
        return null;
      }
    };
    assertThat(interfacePacket.version()).isEqualTo((short) 1);
  }

  @Test
  public void testSafeSequence() {
    final StreamPacketBuilder builder = StreamPacket.builder()
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        .sequence(UnsignedLong.ZERO)
        .prepareAmount(UnsignedLong.ZERO);

    assertThat(builder.sequence(UnsignedLong.ZERO).build().sequenceIsSafeForSingleSharedSecret()).isTrue();
    assertThat(builder.sequence(UnsignedLong.ONE).build().sequenceIsSafeForSingleSharedSecret()).isTrue();
    assertThat(builder.sequence(StreamConnection.MAX_FRAMES_PER_CONNECTION.minus(UnsignedLong.ONE)).build()
        .sequenceIsSafeForSingleSharedSecret()).isTrue();
    assertThat(
        builder.sequence(StreamConnection.MAX_FRAMES_PER_CONNECTION).build().sequenceIsSafeForSingleSharedSecret())
        .isFalse();
    assertThat(builder.sequence(StreamConnection.MAX_FRAMES_PER_CONNECTION.plus(UnsignedLong.ONE)).build()
        .sequenceIsSafeForSingleSharedSecret()).isFalse();
    assertThat(builder.sequence(UnsignedLong.MAX_VALUE).build().sequenceIsSafeForSingleSharedSecret()).isFalse();
  }
}
