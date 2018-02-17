package org.interledger.encoding.asn.codecs;

import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * An ASN.1 codec for ASN.1 objects that extend PrintableString
 */
public abstract class AsnPrintableStringBasedObjectCodec<T>
    extends AsnCharStringBasedObjectCodec<T> {

  private static final String REGEX = "[\\p{Alnum}'()+,-.?:/= ]+";
  private static final Predicate<String> MATCHER = Pattern.compile(REGEX).asPredicate();

  public AsnPrintableStringBasedObjectCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.US_ASCII);
    setValidator(MATCHER);
  }

}
