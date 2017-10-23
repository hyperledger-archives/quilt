package org.interledger.mocks;

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
