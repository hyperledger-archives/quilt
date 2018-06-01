package org.interledger.btp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BtpMessageTypeTest {

  @Test
  public void fromCode() {

    for (BtpMessageType type : BtpMessageType.values()) {
      assertEquals(type, BtpMessageType.fromCode(type.getCode()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromNegativeCode() {
    BtpMessageType.fromCode(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromZeroCode() {
    BtpMessageType.fromCode(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromInvalidCode() {
    BtpMessageType.fromCode(Integer.MAX_VALUE);
  }

}