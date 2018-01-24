package org.interledger.codecs.asn;

import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * An ASN.1 IA5String represented internally as {@link String}.
 */
public class AsnPrintableString extends AsnPrintableStringBasedObject<String> {

  public AsnPrintableString(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint);
  }

  @Override
  protected String decode() {
    return getCharString();
  }

  @Override
  protected void encode(String value) {
    setCharString(value);
  }

  @Override
  public String toString() {
    return "PrintableString{"
        + "value='" + getValue() + '\''
        + '}';
  }
}
