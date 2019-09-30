package org.interledger.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class WrapperTest {

  @Test
  public void wrapThis() {
    Wrapper<Integer> wrapper = new IntWrapper(42);

    assertThat(wrapper).isEqualTo(wrapper);
    assertThat(wrapper.equals(null)).isFalse();
    assertThat(wrapper.equals(new IntWrapper(39))).isFalse();
    assertThat(wrapper).isEqualTo(new IntWrapper(42));
    assertThat(wrapper.hashCode()).isEqualTo(42);
    assertThat(wrapper.toString()).isEqualTo("IntWrapper(42)");
  }

  private final class IntWrapper extends Wrapper<Integer> {

    private int value;

    private IntWrapper(int value) {
      this.value = value;
    }

    @Override
    public Integer value() {
      return value;
    }
  }
}
