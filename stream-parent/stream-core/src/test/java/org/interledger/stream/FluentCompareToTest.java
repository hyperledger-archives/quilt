package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.FluentCompareTo.is;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

public class FluentCompareToTest {

  @Test
  public void isEqualTo() {
    assertThat(is(UnsignedLong.ZERO).equalTo(UnsignedLong.ZERO)).isTrue();
    assertThat(is(UnsignedLong.ZERO).equalTo(UnsignedLong.ONE)).isFalse();
    assertThat(is("foo").equalTo("foo")).isTrue();
    assertThat(is("foo").equalTo("bar")).isFalse();
  }

  @Test
  public void isLessThan() {
    assertThat(is(UnsignedLong.ZERO).lessThan(UnsignedLong.ZERO)).isFalse();
    assertThat(is(UnsignedLong.ZERO).lessThan(UnsignedLong.ONE)).isTrue();
    assertThat(is(UnsignedLong.ONE).lessThan(UnsignedLong.ZERO)).isFalse();
  }

  @Test
  public void isLessThanOrEqualTo() {
    assertThat(is(UnsignedLong.ZERO).lessThanOrEqualTo(UnsignedLong.ZERO)).isTrue();
    assertThat(is(UnsignedLong.ZERO).lessThanOrEqualTo(UnsignedLong.ONE)).isTrue();
    assertThat(is(UnsignedLong.ONE).lessThanOrEqualTo(UnsignedLong.ZERO)).isFalse();
  }

  @Test
  public void isGreaterThan() {
    assertThat(is(UnsignedLong.ZERO).greaterThan(UnsignedLong.ZERO)).isFalse();
    assertThat(is(UnsignedLong.ZERO).greaterThan(UnsignedLong.ONE)).isFalse();
    assertThat(is(UnsignedLong.ONE).greaterThan(UnsignedLong.ZERO)).isTrue();
  }

  @Test
  public void isGreaterThanOrEqualTo() {
    assertThat(is(UnsignedLong.ZERO).greaterThanEqualTo(UnsignedLong.ZERO)).isTrue();
    assertThat(is(UnsignedLong.ZERO).greaterThanEqualTo(UnsignedLong.ONE)).isFalse();
    assertThat(is(UnsignedLong.ONE).greaterThanEqualTo(UnsignedLong.ZERO)).isTrue();
  }

  @Test
  public void isBetween() {
    assertThat(is(UnsignedLong.ZERO).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isTrue();
    assertThat(is(UnsignedLong.ONE).between(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isTrue();
    assertThat(is(UnsignedLong.ONE).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(is(UnsignedLong.ZERO).between(UnsignedLong.ONE, UnsignedLong.MAX_VALUE)).isFalse();
    assertThat(is(UnsignedLong.MAX_VALUE).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
  }

  @Test
  public void isBetweenExclusive() {
    assertThat(is(UnsignedLong.ZERO).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(is(UnsignedLong.ONE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isTrue();
    assertThat(is(UnsignedLong.ONE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(is(UnsignedLong.ZERO).betweenExclusive(UnsignedLong.ONE, UnsignedLong.MAX_VALUE)).isFalse();
    assertThat(is(UnsignedLong.MAX_VALUE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
  }

}