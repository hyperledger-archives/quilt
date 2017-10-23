package org.interledger.cryptoconditions.der;

public enum DerTag {

  BOOLEAN(0x01),
  INTEGER(0x02),
  BIT_STRING(0x03),
  OCTET_STRING(0x04),
  NULL(0x05),
  OBJECT_IDENTIFIER(0x06),
  EXTERNAL(0x08),
  ENUMERATED(0x0a),
  SEQUENCE(0x10),
  SEQUENCE_OF(0x10),
  SET(0x11),
  SET_OF(0x11),
  NUMERIC_STRING(0x12),
  PRINTABLE_STRING(0x13),
  T61_STRING(0x14),
  VIDEOTEX_STRING(0x15),
  IA5_STRING(0x16),
  UTC_TIME(0x17),
  GENERALIZED_TIME(0x18),
  GRAPHIC_STRING(0x19),
  VISIBLE_STRING(0x1a),
  GENERAL_STRING(0x1b),
  UNIVERSAL_STRING(0x1c),
  BMP_STRING(0x1e),
  UTF8_STRING(0x0c),
  CONSTRUCTED(0x20),
  APPLICATION(0x40),
  TAGGED(0x80);

  private int tag;

  private DerTag(int tag) {
    this.tag = tag;
  }

  public int getTag() {
    return this.tag;
  }

}
