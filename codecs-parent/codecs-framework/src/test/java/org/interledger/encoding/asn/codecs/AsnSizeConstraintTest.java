package org.interledger.encoding.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link AsnSizeConstraint}.
 */
public class AsnSizeConstraintTest {

  @Test
  public void testUnconstrained() {
    final AsnSizeConstraint constraint = AsnSizeConstraint.UNCONSTRAINED;

    assertThat(constraint.isUnconstrained()).isTrue();
    assertThat(constraint.isFixedSize()).isFalse();
    assertThat(constraint.getMin()).isEqualTo(0);
    assertThat(constraint.getMax()).isEqualTo(0);
  }

  @Test
  public void isFixedSize() {
    final AsnSizeConstraint constraint = new AsnSizeConstraint(1);

    assertThat(constraint.isUnconstrained()).isFalse();
    assertThat(constraint.isFixedSize()).isTrue();
    assertThat(constraint.getMin()).isEqualTo(1);
    assertThat(constraint.getMax()).isEqualTo(1);
  }


  @Test
  public void isFixedSizeOther() {
    final AsnSizeConstraint constraint = new AsnSizeConstraint(1, 1);

    assertThat(constraint.isUnconstrained()).isFalse();
    assertThat(constraint.isFixedSize()).isTrue();
    assertThat(constraint.getMin()).isEqualTo(1);
    assertThat(constraint.getMax()).isEqualTo(1);
  }

  @Test
  public void isNotFixedSize() {
    final AsnSizeConstraint constraint = new AsnSizeConstraint(1, 2);

    assertThat(constraint.isUnconstrained()).isFalse();
    assertThat(constraint.isFixedSize()).isFalse();
    assertThat(constraint.getMin()).isEqualTo(1);
    assertThat(constraint.getMax()).isEqualTo(2);
  }

  @Test
  public void equalsHashCode() {
    assertThat(AsnSizeConstraint.UNCONSTRAINED).isEqualTo(AsnSizeConstraint.UNCONSTRAINED);
    assertThat(AsnSizeConstraint.UNCONSTRAINED).isNotEqualTo(new AsnSizeConstraint(1));

    assertThat(AsnSizeConstraint.UNCONSTRAINED.hashCode()).isEqualTo(AsnSizeConstraint.UNCONSTRAINED.hashCode());
    assertThat(AsnSizeConstraint.UNCONSTRAINED.hashCode()).isNotEqualTo(new AsnSizeConstraint(1).hashCode());
  }

}
