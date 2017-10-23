package org.interledger.cryptoconditions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.EnumSet;

/**
 * JUnit tests to exercise the {@link CryptoConditionType} class.
 */
public class ConditionTypeTest {

  @Test
  public void test_fromString_case_insensitive() {
    assertEquals(CryptoConditionType.PREFIX_SHA256, CryptoConditionType.fromString("PREFIX-SHA-256"));
    assertEquals(CryptoConditionType.PREFIX_SHA256, CryptoConditionType.fromString("prefix-SHA-256"));
    assertEquals(CryptoConditionType.PREFIX_SHA256, CryptoConditionType.fromString("prefix-sha-256"));
    assertEquals(CryptoConditionType.ED25519_SHA256, CryptoConditionType.fromString("ED25519-SHA-256"));
    assertEquals(CryptoConditionType.ED25519_SHA256, CryptoConditionType.fromString("ed25519-sha-256"));
    assertEquals(CryptoConditionType.ED25519_SHA256, CryptoConditionType.fromString("ED25519-sha-256"));
  }

  @Test
  public void test_getEnumOfTypesAsBitString_None() {
    EnumSet<CryptoConditionType> set = EnumSet.noneOf(CryptoConditionType.class);

    byte[] bitSet = CryptoConditionType.getEnumOfTypesAsBitString(set);

    assertNotNull(bitSet);
    assertEquals(1, bitSet.length);
    assertEquals(0, bitSet[0]);
  }

  @Test
  public void test_getEnumOfTypesAsBitString_All() {
    EnumSet<CryptoConditionType> set = EnumSet.allOf(CryptoConditionType.class);

    byte[] bitSet = CryptoConditionType.getEnumOfTypesAsBitString(set);

    assertNotNull(bitSet);
    assertEquals(2, bitSet.length);
    // the bit string should be '11111', right padded to 8 bits. the first byte is the pad length
    assertEquals(3, bitSet[0]);
    assertEquals(0xF8, Byte.toUnsignedInt(bitSet[1]));
  }

  @Test
  public void test_getEnumOfTypesAsBitString_Lsb() {
    EnumSet<CryptoConditionType> set = EnumSet.of(CryptoConditionType.PREIMAGE_SHA256);

    byte[] bitSet = CryptoConditionType.getEnumOfTypesAsBitString(set);

    assertNotNull(bitSet);
    assertEquals(2, bitSet.length);
    // the bit string should be '1', right padded to 8 bits. the first byte is the pad length
    assertEquals(7, bitSet[0]);
    assertEquals(0x80, Byte.toUnsignedInt(bitSet[1]));
  }

  @Test
  public void test_getEnumOfTypesAsBitString_Msb() {
    EnumSet<CryptoConditionType> set = EnumSet.of(CryptoConditionType.ED25519_SHA256);

    byte[] bitSet = CryptoConditionType.getEnumOfTypesAsBitString(set);

    assertNotNull(bitSet);
    assertEquals(2, bitSet.length);
    // the bit string should be '00001', right padded to 8 bits. the first byte is the pad length
    assertEquals(3, bitSet[0]);
    assertEquals(0x08, Byte.toUnsignedInt(bitSet[1]));
  }

  @Test
  public void test_getEnumOfTypesFromBitString_All() {
    EnumSet<CryptoConditionType> set =
        CryptoConditionType.getEnumOfTypesFromBitString(new byte[] {0x03, (byte) 0xF8});

    assertNotNull(set);
    assertEquals(EnumSet.allOf(CryptoConditionType.class), set);
  }

  @Test
  public void test_getEnumOfTypesFromBitString_Msb() {
    EnumSet<CryptoConditionType> set =
        CryptoConditionType.getEnumOfTypesFromBitString(new byte[] {0x03, (byte) 0x08});

    assertNotNull(set);
    assertEquals(1, set.size());
    assertTrue(set.contains(CryptoConditionType.ED25519_SHA256));
  }

  @Test
  public void test_getEnumOfTypesFromBitString_Lsb() {
    EnumSet<CryptoConditionType> set =
        CryptoConditionType.getEnumOfTypesFromBitString(new byte[] {0x07, (byte) 0x80});

    assertNotNull(set);
    assertEquals(1, set.size());
    assertTrue(set.contains(CryptoConditionType.PREIMAGE_SHA256));
  }

  @Test
  public void test_getEnumOfTypesAsString_None() {
    EnumSet<CryptoConditionType> set = EnumSet.noneOf(CryptoConditionType.class);
    String typeString = CryptoConditionType.getEnumOfTypesAsString(set);

    assertNotNull(typeString);
    assertEquals("", typeString);
  }

  @Test
  public void test_getEnumOfTypesAsString_All() {
    EnumSet<CryptoConditionType> set = EnumSet.allOf(CryptoConditionType.class);
    String typeString = CryptoConditionType.getEnumOfTypesAsString(set);

    assertNotNull(typeString);
    assertEquals("ed25519-sha-256,prefix-sha-256,preimage-sha-256,rsa-sha-256,threshold-sha-256",
        typeString);
  }

  @Test
  public void test_getEnumOfTypesFromString_None() {
    EnumSet<CryptoConditionType> set = CryptoConditionType.getEnumOfTypesFromString("");

    assertNotNull(set);
    assertTrue(set.isEmpty());
  }

  @Test
  public void test_getEnumOfTypesFromString_All() {
    String list = "preimage-sha-256,prefix-sha-256,threshold-sha-256,rsa-sha-256,ed25519-sha-256";
    EnumSet<CryptoConditionType> set = CryptoConditionType.getEnumOfTypesFromString(list);

    assertNotNull(set);
    assertEquals(EnumSet.allOf(CryptoConditionType.class), set);
  }

}

