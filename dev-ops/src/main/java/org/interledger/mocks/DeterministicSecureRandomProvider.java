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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;

/**
 * A PRNG that always returns a deterministic outcome for use in testing.
 */
public class DeterministicSecureRandomProvider extends Provider {

  private static final long serialVersionUID = 5364295470454144673L;

  private static final String PROVIDER_NAME = "DeterministicSecureRandomProvider";
  private static final String ALGO_NAME = "DeterministicPRNG";


  public static byte[] SEED;
  public static String PREVIOUS_STRONG_ALGO;

  protected DeterministicSecureRandomProvider(byte[] seed) {
    super(PROVIDER_NAME, 1.0, null);
    SEED = seed;
    put("SecureRandom." + ALGO_NAME, DeterministicSecureRandomAlgorithm.class.getName());
  }

  /**
   * Set this provider as the default PRNG provider.
   *
   * @param seed The seed to use for any calls to the provider
   */
  public static void setAsDefault(byte[] seed) {

    PREVIOUS_STRONG_ALGO = AccessController.doPrivileged(
        (PrivilegedAction<String>) () -> Security.getProperty("securerandom.strongAlgorithms"));

    Security.insertProviderAt(new DeterministicSecureRandomProvider(seed), 1);

    AccessController.doPrivileged((PrivilegedAction<String>) () -> {

      final String property = ALGO_NAME + ":" + PROVIDER_NAME;

      Security.setProperty("securerandom.strongAlgorithms", property);
      return property;

    });
  }

  /**
   * Remove this provider and revert the security settings.
   */
  public static void remove() {

    AccessController.doPrivileged((PrivilegedAction<String>) () -> {

      Security.setProperty("securerandom.strongAlgorithms", PREVIOUS_STRONG_ALGO);
      return PREVIOUS_STRONG_ALGO;

    });

    Security.removeProvider(PROVIDER_NAME);
  }

}
