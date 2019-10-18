package org.interledger.ildcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import org.interledger.core.DateUtils;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.time.Instant;

public class IldcpRequestTest {

  @Test
  public void defaults() throws Exception {
    final Instant nowPlus30 = DateUtils.now().plusSeconds(30);
    Thread.sleep(1);
    final IldcpRequest request = IldcpRequest.builder().build();
    Thread.sleep(1);
    Instant afterPlus30 = DateUtils.now().plusSeconds(30);
    assertThat(request.getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(spy(IldcpRequest.class).getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(request.getExpiresAt()).isAfter(nowPlus30);
    assertThat(request.getExpiresAt()).isBefore(afterPlus30);
  }

}
