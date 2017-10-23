package org.interledger.cryptoconditions.helpers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class that models the "json" field in the testVectorData file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestVectorJson {

  private int maxMessageLength;
  private String modulus;
  private String prefix;
  private String preimage;
  private String publicKey;
  private String signature;
  private TestVectorJson subfulfillment;
  private TestVectorJson[] subfulfillments;
  private int threshold;
  private String type;

  // Debug info
  private String fingerprintContents;
  private String conditionBinary;

  @JsonProperty
  public String getFingerprintContents() {
    return fingerprintContents;
  }

  public void setFingerprintContents(String fingerprintContents) {
    this.fingerprintContents = fingerprintContents;
  }

  @JsonProperty
  public String getConditionBinary() {
    return conditionBinary;
  }

  public void setConditionBinary(String conditionBinary) {
    this.conditionBinary = conditionBinary;
  }

  @JsonProperty
  public int getMaxMessageLength() {
    return maxMessageLength;
  }

  @JsonProperty
  public String getModulus() {
    return modulus;
  }

  @JsonProperty
  public String getPrefix() {
    return prefix;
  }

  @JsonProperty
  public String getPreimage() {
    return preimage;
  }


  @JsonProperty
  public String getPublicKey() {
    return publicKey;
  }

  @JsonProperty
  public String getSignature() {
    return signature;
  }

  @JsonProperty
  public TestVectorJson getSubfulfillment() {
    return subfulfillment;
  }

  @JsonProperty
  public TestVectorJson[] getSubfulfillments() {
    return subfulfillments;
  }

  @JsonProperty
  public int getThreshold() {
    return threshold;
  }

  @JsonProperty
  public String getType() {
    return type;
  }

  public void setMaxMessageLength(int maxMessageLength) {
    this.maxMessageLength = maxMessageLength;
  }

  public void setModulus(String modulus) {
    this.modulus = modulus;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setPreimage(String preimage) {
    this.preimage = preimage;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public void setSubfulfillment(TestVectorJson subfulfillment) {
    this.subfulfillment = subfulfillment;
  }

  public void setSubfulfillments(TestVectorJson[] subfulfillments) {
    this.subfulfillments = subfulfillments;
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  public void setType(String type) {
    this.type = type;
  }
}
