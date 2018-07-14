package org.interledger.quilt.jackson.conditions;

/*-
 * ========================LICENSE_START=================================
 * Interledger Jackson Datatypes
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

import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;

/**
 * Indicates the type of encoding and decoding to use when serializing and deserializing a {@link
 * InterledgerCondition} or {@link InterledgerFulfillment}.
 */
public enum Encoding {
  HEX,
  BASE64,
  BASE64_WITHOUT_PADDING,
  BASE64URL,
  BASE64URL_WITHOUT_PADDING
}
