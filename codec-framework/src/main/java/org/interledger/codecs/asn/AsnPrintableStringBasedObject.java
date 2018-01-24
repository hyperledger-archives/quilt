package org.interledger.codecs.asn;

import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * An ASN.1 IA5String represented internally as {@link String}.
 */
public abstract class AsnPrintableStringBasedObject<T> extends AsnCharStringBasedObject<T> {

  private static final String REGEX = "[\\p{Alnum}'()+,-.?:/= ]+";
  private static final Predicate<String> MATCHER = Pattern.compile(REGEX).asPredicate();

  public AsnPrintableStringBasedObject(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.US_ASCII);
    setValidator(MATCHER);
  }

}
