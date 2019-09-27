package org.interledger.link;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

public class LinkTest {

  @Test
  public void defaults() {
    assertThat(spy(Link.class).getOperatorAddressSupplier().get()).isEqualTo(Link.SELF);
  }
}
