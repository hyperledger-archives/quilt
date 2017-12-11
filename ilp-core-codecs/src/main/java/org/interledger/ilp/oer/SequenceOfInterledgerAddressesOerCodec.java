package org.interledger.ilp.oer;

import org.interledger.InterledgerAddress;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.oer.OerSequenceOfCodec;

import java.util.List;


public class SequenceOfInterledgerAddressesOerCodec extends OerSequenceOfCodec<InterledgerAddress>
    implements Codec<List<InterledgerAddress>> {
  public SequenceOfInterledgerAddressesOerCodec() {
    super(new InterledgerAddressOerCodec());
  }

}
