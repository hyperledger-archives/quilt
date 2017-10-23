package org.interledger.cryptoconditions.helpers;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A helper class used in the RSA tests to hold JSON data for testing purposes.
 */
public class CaseJson {

  private String message;
  private String salt;
  private String signature;

  @JsonProperty
  public String getMessage() {
    return message;
  }

  public CaseJson setMessage(String message) {
    this.message = message;
    return this;
  }

  @JsonProperty
  public String getSalt() {
    return salt;
  }

  public CaseJson setSalt(String salt) {
    this.salt = salt;
    return this;
  }

  @JsonProperty
  public String getSignature() {
    return signature;
  }

  public CaseJson setSignature(String signature) {
    this.signature = signature;
    return this;
  }
}
