package org.interledger.btp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BtpSubProtocolContentTypeTest {

  @Test
  public void fromCode() {

    for (BtpSubProtocolContentType code : BtpSubProtocolContentType.values()) {
      assertEquals(code, BtpSubProtocolContentType.fromCode(code.getCode()));
    }
  }


  @Test(expected = IllegalArgumentException.class)
  public void fromNegativeCode() {
    BtpSubProtocolContentType.fromCode(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromInvalidCode() {
    BtpSubProtocolContentType.fromCode(Integer.MAX_VALUE);
  }
}