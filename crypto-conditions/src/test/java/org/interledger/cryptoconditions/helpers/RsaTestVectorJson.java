package org.interledger.cryptoconditions.helpers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A helper class used in the RSA tests to hold JSON data for testing purposes.
 */
public class RsaTestVectorJson {

  private String modulus;
  private String privateKey;
  private List<CaseJson> cases;

  @JsonProperty
  public String getModulus() {
    return modulus;
  }

  public RsaTestVectorJson setModulus(String modulus) {
    this.modulus = modulus;
    return this;
  }

  @JsonProperty
  public String getPrivateKey() {
    return privateKey;
  }

  public RsaTestVectorJson setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  @JsonProperty
  public List<CaseJson> getCases() {
    return cases;
  }

  public RsaTestVectorJson setCases(List<CaseJson> cases) {
    this.cases = cases;
    return this;
  }
}
