package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;

public class UnsignedLongUtils {

  public static UnsignedLongEvaluator is(UnsignedLong value) {
    return new UnsignedLongEvaluator(value);
  }

  public static class UnsignedLongEvaluator {
    private final UnsignedLong value;

    public UnsignedLongEvaluator(UnsignedLong value) {
      this.value = value;
    }

    public boolean equalTo(UnsignedLong other) {
      return value.equals(other);
    }

    public boolean lessThan(UnsignedLong other) {
      return value.compareTo(other) < 0;
    }

    public boolean lessThanOrEqualTo(UnsignedLong other) {
      return value.compareTo(other) <= 0;
    }

    public boolean greaterThan(UnsignedLong other) {
      return value.compareTo(other) > 0;
    }

    public boolean greaterThanEqualTo(UnsignedLong other) {
      return value.compareTo(other) >= 0;
    }

  }
}
