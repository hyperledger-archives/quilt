package org.interledger.core.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

/**
 * Unit tests for {@link FluentPicker}.
 */
public class FluentPickerTest {

  @Test
  public void min() {
    assertThat(FluentPicker.min(UnsignedLong.ONE, UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);
    assertThat(FluentPicker.min(UnsignedLong.ONE, UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(FluentPicker.min(UnsignedLong.ZERO, UnsignedLong.ONE)).isEqualTo(UnsignedLong.ZERO);
    assertThat(FluentPicker.min(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void max() {
    assertThat(FluentPicker.max(UnsignedLong.ONE, UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);
    assertThat(FluentPicker.max(UnsignedLong.ONE, UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ONE);
    assertThat(FluentPicker.max(UnsignedLong.ZERO, UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);
    assertThat(FluentPicker.max(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isEqualTo(UnsignedLong.MAX_VALUE);
  }
}