package org.interledger.encoding.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

/**
 * A simple data structure for representing ASN.1 size constraints.
 */
public class AsnSizeConstraint {

  /**
   * A {@link AsnSizeConstraint} that represents an unconstrained size.
   *
   * <p>The following {@code AsnSizeConstraint.UNCONSTRAINED.isUnconstrained()} always returns true.
   */
  public static final AsnSizeConstraint UNCONSTRAINED = new AsnSizeConstraint(0, 0);
  private final int min;
  private final int max;

  /**
   * Create a new size constraint for a fixed sized field.
   *
   * @param fixedSize the size of the field
   */
  public AsnSizeConstraint(int fixedSize) {
    this.min = fixedSize;
    this.max = fixedSize;
  }

  /**
   * Create a new size constraint for a variable length field.
   *
   * @param min The minimum size of the field.
   * @param max The maximum size of the field.
   */
  public AsnSizeConstraint(int min, int max) {
    this.min = min;
    this.max = max;
  }

  /**
   * A flag indicating if this object represents a size constraint or not.
   *
   * @return false if this object represents a constrained size.
   */
  public boolean isUnconstrained() {
    return max == 0 && min == 0;
  }

  /**
   * A flag indicating if this object represents a fixed size constraint.
   *
   * @return true if the object has a fixed size.
   */
  public boolean isFixedSize() {
    return max != 0 && max == min;
  }

  /**
   * Get the maximum size represented by this constraint.
   *
   * @return the max size or 0 if {@link #isUnconstrained()} is true.
   */
  public int getMax() {
    return max;
  }

  /**
   * Get the minimum size represented by this constraint.
   *
   * @return the min size or 0 if {@link #isUnconstrained()} is true.
   */
  public int getMin() {
    return min;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AsnSizeConstraint that = (AsnSizeConstraint) obj;

    if (min != that.min) {
      return false;
    }
    return max == that.max;
  }

  @Override
  public int hashCode() {
    int result = min;
    result = 31 * result + max;
    return result;
  }
}
