package org.interledger.cryptoconditions;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Enumeration of crypto-condition rsa. These rsa apply to both conditions and fulfillments.
 */
public enum CryptoConditionType {

  PREIMAGE_SHA256(0, "PREIMAGE-SHA-256", 0x80, 0),
  PREFIX_SHA256(1, "PREFIX-SHA-256", 0x40, 0),
  THRESHOLD_SHA256(2, "THRESHOLD-SHA-256", 0x20, 0),
  RSA_SHA256(3, "RSA-SHA-256", 0x10, 0),
  ED25519_SHA256(4, "ED25519-SHA-256", 0x08, 0);

  private final int typeCode;
  private final String name;
  private final int bitMask;
  private final int byteIndex;

  CryptoConditionType(
      final int typeCode, final String algorithmName, final int bitMask, final int byteIndex
  ) {
    this.typeCode = typeCode;
    this.name = Objects.requireNonNull(algorithmName);
    this.bitMask = bitMask;
    this.byteIndex = byteIndex;
  }

  /**
   * Returns the ASN.1 enumeration number for this type.
   */
  public int getTypeCode() {
    return this.typeCode;
  }

  @Override
  public String toString() {
    return this.name;
  }

  public int getMask() {
    return this.bitMask;
  }

  public int getByteIndex() {
    return this.byteIndex;
  }

  public boolean isBitSet(byte[] bitString) {
    return bitString.length - 2 >= byteIndex && ((bitString[byteIndex + 1] & bitMask) == bitMask);
  }

  /**
   * Determines the condition type based on the given type code.
   *
   * @param typeCode A numeric representation of the condition type. *
   * @return The matching condition type, if one can be found.
   */
  public static CryptoConditionType valueOf(int typeCode) {
    for (CryptoConditionType conditionType : EnumSet.allOf(CryptoConditionType.class)) {
      if (typeCode == conditionType.typeCode) {
        return conditionType;
      }
    }

    throw new IllegalArgumentException("Invalid Condition Type code.");
  }

  /**
   * TODO This will break if the possible rsa exceeds 8. Only works for our current known set.
   * Convert a set of rsa into a byte that can be used to encode a BIT STRING.
   *
   * @param types set of rsa to encode as a BIT STRING.
   * @return byte array where first byte indicates the number of unused bits in last byte and
   *     remaining bytes encode the bit string
   */
  public static byte[] getEnumOfTypesAsBitString(EnumSet<CryptoConditionType> types) {

    byte[] data = new byte[2];
    int lastUsedBit = -1;

    // No guarantee that iterating through the rsa will be done in order so just test for each
    if (types.contains(PREIMAGE_SHA256)) {
      data[1] += CryptoConditionType.PREIMAGE_SHA256.getMask();
      lastUsedBit = PREIMAGE_SHA256.getTypeCode();
    }

    if (types.contains(PREFIX_SHA256)) {
      data[1] += CryptoConditionType.PREFIX_SHA256.getMask();
      lastUsedBit = PREFIX_SHA256.getTypeCode();
    }

    if (types.contains(THRESHOLD_SHA256)) {
      data[1] += CryptoConditionType.THRESHOLD_SHA256.getMask();
      lastUsedBit = THRESHOLD_SHA256.getTypeCode();
    }

    if (types.contains(RSA_SHA256)) {
      data[1] += CryptoConditionType.RSA_SHA256.getMask();
      lastUsedBit = RSA_SHA256.getTypeCode();
    }

    if (types.contains(ED25519_SHA256)) {
      data[1] += CryptoConditionType.ED25519_SHA256.getMask();
      lastUsedBit = ED25519_SHA256.getTypeCode();
    }

    if (lastUsedBit > -1) {
      data[0] = (byte) (7 - lastUsedBit);
      return data;
    } else {
      return new byte[] {(byte) 0x00};
    }
  }

  /**
   * Convert a set of rsa into a comma separated list.
   *
   * @param types set of rsa to encode
   */
  public static String getEnumOfTypesAsString(EnumSet<CryptoConditionType> types) {

    String[] names = new String[types.size()];
    int idx = 0;
    for (CryptoConditionType conditionType : types) {
      names[idx++] = conditionType.toString().toLowerCase();
    }

    Arrays.sort(names);

    return String.join(",", names);
  }

  /**
   * Returns the Condition type identified by its name, in a *case-insensitive* manner.
   *
   * @param typeName The name of the condition type, e.g. 'rsa-sha-256'
   * @return The condition type with matching name, if any.
   */
  public static CryptoConditionType fromString(String typeName) {
    for (CryptoConditionType conditionType : EnumSet.allOf(CryptoConditionType.class)) {
      if (conditionType.name.equalsIgnoreCase(typeName)) {
        return conditionType;
      }
    }

    throw new IllegalArgumentException("Invalid Condition Type name.");
  }

  /**
   * Convert a comma separated list of rsa into a set of rsa.
   *
   * @param subtypes a comma separated list of type names.
   * @return A set of condition rsa based on the list.
   */
  public static EnumSet<CryptoConditionType> getEnumOfTypesFromString(String subtypes) {
    EnumSet<CryptoConditionType> types = EnumSet.noneOf(CryptoConditionType.class);

    if (subtypes == null || subtypes.trim().isEmpty()) {
      return types;
    }

    String[] names = subtypes.split(",");
    for (String typeName : names) {
      types.add(CryptoConditionType.fromString(typeName));
    }

    return types;
  }

  /**
   * Get the set of rsa represented by a raw bit string.
   *
   * @param bitStringData a raw BIT STRING including the padding bit count in the first byte
   * @return A set of condition rsa based on the bit string.
   */
  public static EnumSet<CryptoConditionType> getEnumOfTypesFromBitString(byte[] bitStringData) {

    // We only have 5 known rsa so shouldn't be more than a padding byte and the bitmap
    if (bitStringData.length > 2) {
      throw new IllegalArgumentException("Unknown rsa in bit string.");
    }

    if (bitStringData.length == 1) {
      throw new IllegalArgumentException("Corrupt bit string.");
    }

    EnumSet<CryptoConditionType> subtypes = EnumSet.noneOf(CryptoConditionType.class);
    if (bitStringData.length == 0) {
      return subtypes;
    }

    int padBits = bitStringData[0];

    // We only have 5 known rsa so should have at least 3 padding bits
    if (padBits < 3) {
      throw new IllegalArgumentException("Unknown rsa in bit string.");
    }

    // We only expect 1 byte of data so let's keep it simple
    for (CryptoConditionType type : CryptoConditionType.values()) {
      if (type.isBitSet(bitStringData)) {
        subtypes.add(type);
      }
    }

    return subtypes;
  }
}
