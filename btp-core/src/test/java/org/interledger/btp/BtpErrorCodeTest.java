package org.interledger.btp;

import static org.junit.Assert.*;

import org.junit.Test;

public class BtpErrorCodeTest {

  @Test
  public void fromString() {

    for (BtpErrorCode code : BtpErrorCode.values()) {
      assertEquals(code, BtpErrorCode.fromString(code.getCode()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromBadString() {
    BtpErrorCode.fromString("TEST");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromEmptyString() {
    BtpErrorCode.fromString("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromNullString() {
    BtpErrorCode.fromString(null);
  }

}