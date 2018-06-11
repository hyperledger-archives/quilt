package org.interledger.ildcp;

/*-
 * ========================LICENSE_START=================================
 * Interledger Dynamic Configuration Protocol Core Libs
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

import org.interledger.annotations.Immutable;
import org.interledger.core.InterledgerAddress;

import javax.money.CurrencyUnit;

public interface IldcpResponse {

  static IldcpResponseBuilder builder() {
    return new IldcpResponseBuilder();
  }

  /**
   * The Interledger address of the account assigned by the configuration provider.
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress getInterledgerAddress();

  /**
   * The currency unit of the asset underlying this account. (Despite the name of the returned
   * object, this value may represent a non-currency asset).
   *
   * @return A {@link CurrencyUnit}.
   */
  CurrencyUnit getCurrencyUnit();

  /**
   * <p>The order of magnitude to express one full currency unit in this account's base units. More
   * formally, an integer (..., -2, -1, 0, 1, 2, ...), such that one of the account's base units
   * equals 10^-<tt>currencyScale</tt> <tt>currencyCode</tt></p>
   *
   * <p>For example, if the integer values represented on the system are to be interpreted
   * as dollar-cents (for the purpose of settling a user's account balance, for instance), then the
   * account's currencyScale is 2. The amount 10000 would be translated visually into a decimal
   * format via the following equation: 10000 * (10^(-2)), or "100.00".</p>
   *
   * @return An {@link Integer} representing the scale.
   */
  Integer getCurrencyScale();

  @Immutable
  abstract class AbstractIldcpResponse implements IldcpResponse {

  }

}
