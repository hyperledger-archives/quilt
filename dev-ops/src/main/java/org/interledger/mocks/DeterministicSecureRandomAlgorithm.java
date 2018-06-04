package org.interledger.mocks;

/*-
 * ========================LICENSE_START=================================
 * Hyperledger Quilt Dev-Ops Tools
 * %%
 * Copyright (C) 2017 - 2018 Interledger
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

import java.security.SecureRandomSpi;

public class DeterministicSecureRandomAlgorithm extends SecureRandomSpi {

  private static final long serialVersionUID = 8914286719772377141L;

  @Override
  protected void engineSetSeed(byte[] seed) {
    for (int i = 0; i < seed.length; i++) {
      seed[i] =
          DeterministicSecureRandomProvider.SEED[i % DeterministicSecureRandomProvider.SEED.length];
    }
  }

  @Override
  protected void engineNextBytes(byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] =
          DeterministicSecureRandomProvider.SEED[i % DeterministicSecureRandomProvider.SEED.length];
    }
  }

  @Override
  protected byte[] engineGenerateSeed(int numBytes) {
    byte[] bytes = new byte[numBytes];
    for (int i = 0; i < numBytes; i++) {
      bytes[i] =
          DeterministicSecureRandomProvider.SEED[i % DeterministicSecureRandomProvider.SEED.length];
    }
    return bytes;
  }

}
