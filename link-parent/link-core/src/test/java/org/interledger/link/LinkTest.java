package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import org.junit.Test;

/**
 * Unit tests for {@link Link}.
 */
public class LinkTest {

  @Test
  public void defaults() {
    assertThat(spy(Link.class).getOperatorAddressSupplier().get()).isEqualTo(Link.SELF);
  }

}
