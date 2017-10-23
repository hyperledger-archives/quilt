package org.interledger.psk;

import java.util.Objects;

/**
 * Enumerates the types of encryption that can be used in the Pre-Shared Key Transport Protocol.
 */
public enum PskEncryptionType {
  NONE("none"),
  AES_256_GCM("aes-256-gcm");

  private final String name;

  /**
   * Internal constructor that binds the enum to the description found in PSK message headers.
   *
   * @param name The description of the encryption type found in PSK message headers.
   */
  PskEncryptionType(final String name) {
    this.name = name;
  }

  /**
   * Convenience method to return the {@link PskEncryptionType} that matches the text description.
   *
   * @param description The text description of the encryption type, typically found in PSK message
   *                    headers.
   *
   * @return The matching {@link PskEncryptionType} if one can be found, otherwise null.
   */
  public static PskEncryptionType fromString(final String description) {
    Objects.requireNonNull(description);

    for (PskEncryptionType type : PskEncryptionType.values()) {
      if (type.name.equalsIgnoreCase(description)) {
        return type;
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return name;
  }
}
