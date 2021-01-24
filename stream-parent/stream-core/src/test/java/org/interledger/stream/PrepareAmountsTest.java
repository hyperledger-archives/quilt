package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerPreparePacket;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

/**
 * Unit tests for {@link PrepareAmounts}.
 */
public class PrepareAmountsTest {

  @Test
  public void testFrom() {
    final UnsignedLong amountToSend = UnsignedLong.MAX_VALUE;
    InterledgerPreparePacket preparePacketMock = mock(InterledgerPreparePacket.class);
    when(preparePacketMock.getAmount()).thenReturn(amountToSend);

    final UnsignedLong minimumAmountToAccept = UnsignedLong.ONE;
    StreamPacket streamPacketMock = mock(StreamPacket.class);
    when(streamPacketMock.prepareAmount()).thenReturn(minimumAmountToAccept);

    PrepareAmounts actual = PrepareAmounts.from(preparePacketMock, streamPacketMock);
    assertThat(actual.getAmountToSend()).isEqualTo(amountToSend);
    assertThat(actual.getMinimumAmountToAccept()).isEqualTo(minimumAmountToAccept);
  }
}
