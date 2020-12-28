package org.interledger.core.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

/**
 * Unit tests for {@link FluentCompareTo}.
 */
public class FluentCompareToTest {

  @Test
  public void isEqualTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).equalTo(UnsignedLong.ZERO)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).equalTo(UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is("foo").equalTo("foo")).isTrue();
    assertThat(FluentCompareTo.is("foo").equalTo("bar")).isFalse();
  }

  @Test
  public void isLessThan() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).lessThan(UnsignedLong.ZERO)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).lessThan(UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).lessThan(UnsignedLong.ZERO)).isFalse();
  }

  @Test
  public void isLessThanOrEqualTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).lessThanOrEqualTo(UnsignedLong.ZERO)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).lessThanOrEqualTo(UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).lessThanOrEqualTo(UnsignedLong.ZERO)).isFalse();
  }

  @Test
  public void isGreaterThan() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).greaterThan(UnsignedLong.ZERO)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).greaterThan(UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).greaterThan(UnsignedLong.ZERO)).isTrue();
  }

  @Test
  public void isGreaterThanOrEqualTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).greaterThanEqualTo(UnsignedLong.ZERO)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).greaterThanEqualTo(UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).greaterThanEqualTo(UnsignedLong.ZERO)).isTrue();
  }

  @Test
  public void isBetween() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).between(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).between(UnsignedLong.ONE, UnsignedLong.MAX_VALUE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.MAX_VALUE).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
  }

  @Test
  public void isBetweenExclusive() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE))
      .isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).betweenExclusive(UnsignedLong.ONE, UnsignedLong.MAX_VALUE))
      .isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.MAX_VALUE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE))
      .isFalse();
  }

}