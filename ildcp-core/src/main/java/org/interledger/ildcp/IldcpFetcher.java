package org.interledger.ildcp;

/*-
 * ========================LICENSE_START=================================
 * Interledger DCP Core
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

import org.interledger.core.InterledgerProtocolException;

/**
 * Defines how to fetch IL-DCP data from a remote connector.
 */
public interface IldcpFetcher {

  /**
   * Send an IL-DCP request to a remote connector, and expect a response containing IL-DCP information to configure this
   * caller.
   *
   * @param ildcpRequest An instance of {@link IldcpRequest} that can be used to make a request to a parent Connector.
   *
   * @return An {@link IldcpResponse} fulfillment that contains everything necessary to respond to an IL-DCP response.
   *
   * @throws InterledgerProtocolException If the IL-DCP server could not complete the request.
   */
  IldcpResponse fetch(IldcpRequest ildcpRequest) throws InterledgerProtocolException;

}
