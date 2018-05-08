package org.interledger.encoding.asn.framework;

import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;

import org.hamcrest.MatcherAssert;
import org.junit.Test;


public class OnValueChangedTest {

  @Test
  public void testEvent() {

    final int[] values = new int[]{0};

    AsnUint8Codec codec = new AsnUint8Codec();
    codec.setValueChangedEventListener(source -> {
      values[0] = source.decode();
    });

    codec.encode(1);

    MatcherAssert.assertThat("Event was fired.", values[0] == 1);

  }

  @Test(expected = IllegalStateException.class)
  public void testSetterWithExistingListener() {

    AsnUint8Codec codec = new AsnUint8Codec();
    codec.setValueChangedEventListener(source -> { });
    codec.setValueChangedEventListener(source -> { });

  }

  @Test
  public void testRemove() {

    AsnUint8Codec codec = new AsnUint8Codec();
    codec.setValueChangedEventListener(source -> { });
    codec.removeEncodeEventListener();

    MatcherAssert.assertThat("Listener was removed.", !codec.hasValueChangedEventListener());

  }

}
