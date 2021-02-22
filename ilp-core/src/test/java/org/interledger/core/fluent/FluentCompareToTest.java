package org.interledger.core.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link FluentCompareTo}.
 */
public class FluentCompareToTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  /////////
  // is
  /////////

  @Test
  public void isNull() {
    expectedException.expect(NullPointerException.class);
    FluentCompareTo.is(null);
  }

  @Test
  public void isGetValue() {
    assertThat(FluentCompareTo.is(UnsignedLong.MAX_VALUE).getValue()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  /////////
  // equalTo
  /////////

  @Test
  public void equalTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).equalTo(UnsignedLong.ZERO)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).equalTo(UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is("foo").equalTo("foo")).isTrue();
    assertThat(FluentCompareTo.is("foo").equalTo("bar")).isFalse();
  }

  @Test
  public void notEqualTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notEqualTo(UnsignedLong.ZERO)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notEqualTo(UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is("foo").notEqualTo("foo")).isFalse();
    assertThat(FluentCompareTo.is("foo").notEqualTo("bar")).isTrue();
  }

  /////////
  // lessThan
  /////////

  @Test
  public void lessThan() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).lessThan(UnsignedLong.ZERO)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).lessThan(UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).lessThan(UnsignedLong.ZERO)).isFalse();
  }

  @Test
  public void notLessThan() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notLessThan(UnsignedLong.ZERO)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notLessThan(UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).notLessThan(UnsignedLong.ZERO)).isTrue();
  }

  /////////
  // lessThanOrEqualTo
  /////////

  @Test
  public void lessThanOrEqualTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).lessThanOrEqualTo(UnsignedLong.ZERO)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).lessThanOrEqualTo(UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).lessThanOrEqualTo(UnsignedLong.ZERO)).isFalse();
  }

  @Test
  public void notLessThanOrEqualTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notLessThanOrEqualTo(UnsignedLong.ZERO)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notLessThanOrEqualTo(UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).notLessThanOrEqualTo(UnsignedLong.ZERO)).isTrue();
  }

  /////////
  // greaterThan
  /////////

  @Test
  public void greaterThan() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).greaterThan(UnsignedLong.ZERO)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).greaterThan(UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).greaterThan(UnsignedLong.ZERO)).isTrue();
  }

  @Test
  public void notGreaterThan() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notGreaterThan(UnsignedLong.ZERO)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notGreaterThan(UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).notGreaterThan(UnsignedLong.ZERO)).isFalse();
  }

  /////////
  // greaterThanEqualTo
  /////////

  @Test
  public void greaterThanEqualTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).greaterThanEqualTo(UnsignedLong.ZERO)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).greaterThanEqualTo(UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).greaterThanEqualTo(UnsignedLong.ZERO)).isTrue();
  }

  @Test
  public void notGreaterThanEqualTo() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notGreaterThanEqualTo(UnsignedLong.ZERO)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notGreaterThanEqualTo(UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).notGreaterThanEqualTo(UnsignedLong.ZERO)).isFalse();
  }

  /////////
  // between
  /////////

  @Test
  public void between() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).between(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).between(UnsignedLong.ONE, UnsignedLong.MAX_VALUE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.MAX_VALUE).between(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
  }

  @Test
  public void notBetween() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notBetween(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).notBetween(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).notBetween(UnsignedLong.ZERO, UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notBetween(UnsignedLong.ONE, UnsignedLong.MAX_VALUE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.MAX_VALUE).notBetween(UnsignedLong.ZERO, UnsignedLong.ONE)).isTrue();
  }

  /////////
  // betweenExclusive
  /////////

  @Test
  public void betweenExclusive() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE))
      .isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).betweenExclusive(UnsignedLong.ONE, UnsignedLong.MAX_VALUE))
      .isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.MAX_VALUE).betweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE))
      .isFalse();
  }

  @Test
  public void notBetweenExclusive() {
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notBetweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).notBetweenExclusive(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE))
      .isFalse();
    assertThat(FluentCompareTo.is(UnsignedLong.ONE).notBetweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE)).isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.ZERO).notBetweenExclusive(UnsignedLong.ONE, UnsignedLong.MAX_VALUE))
      .isTrue();
    assertThat(FluentCompareTo.is(UnsignedLong.MAX_VALUE).notBetweenExclusive(UnsignedLong.ZERO, UnsignedLong.ONE))
      .isTrue();
  }

}