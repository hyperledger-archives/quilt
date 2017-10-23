package org.interledger.cryptoconditions.der;

import java.math.BigInteger;

public class DerObject {
  
  private int tag;
  private int length;
  private byte[] value;
  
  public int getTag() {
    return tag;
  }
  
  public int getLength() {
    return length;
  }
  
  public byte[] getValue() {
    return value;
  }
 
  public void setTag(int tag) {
    this.tag = tag;
  }
  
  public void setLength(int length) {
    this.length = length;
  }
  
  public void setValue(byte[] value) {
    this.value = value;
  }
  
  public BigInteger toInteger() {
    return new BigInteger(getValue());
  }
  
}
