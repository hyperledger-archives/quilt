package org.interledger.ildcp.asn.codecs;

import org.interledger.core.asn.codecs.AsnInterledgerAddressCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;

import org.interledger.ildcp.IldcpResponse;

import javax.money.CurrencyContext;
import javax.money.CurrencyContextBuilder;

public class AsnIldcpResponseCodec extends AsnSequenceCodec<IldcpResponse> {


  private final CurrencyContext currencyContext;

  public AsnIldcpResponseCodec() {
    super(
        new AsnInterledgerAddressCodec(),
        new AsnUint8Codec(),
        new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED)
    );

    //TODO Better way to do this?
    currencyContext = CurrencyContextBuilder.of("").build();
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public IldcpResponse decode() {
    return null;
// TODO
//    return IldcpResponse.builder()
//        .interledgerAddress(getValueAt(0))
//        .currencyScale(getValueAt(1))
//        .currencyUnit(CurrencyUnitBuilder.of(getValueAt(2), currencyContext).build())
//        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(IldcpResponse value) {
    setValueAt(0, value.getInterledgerAddress());
    setValueAt(1, value.getCurrencyScale());
    setValueAt(2, value.getCurrencyUnit().getCurrencyCode());
  }
}
