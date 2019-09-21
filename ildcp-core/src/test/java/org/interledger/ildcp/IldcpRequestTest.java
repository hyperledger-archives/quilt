package org.interledger.ildcp;

import org.junit.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

public class IldcpRequestTest {

  @Test
  public void defaults() {
    IldcpRequest request = IldcpRequest.builder().build();
    assertThat(request.getAmount()).isEqualTo(BigInteger.ZERO);
    assertThat(spy(IldcpRequest.class).getAmount()).isEqualTo(BigInteger.ZERO);
  }
}
