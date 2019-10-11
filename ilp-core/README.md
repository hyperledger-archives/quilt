# Interledger Core [![Javadocs](https://www.javadoc.io/badge/org.interledger/ilp-core.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/ilp-core)  [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-core.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-core)

https://www.javadoc.io/doc/org.interledger/ilp-core/1.0.0

Base library for Interledger projects providing service interfaces, event descriptions, exceptions and data models.

For more information about Interledger specifications that underpin this library, please reference [https://github.com/interledger/rfcs](https://github.com/interledger/rfcs).

## Usage
For more detail about how to use this library, consult the [wiki](https://github.com/hyperledger/quilt/wiki)

The library offers interfaces for all core objects and default builders that produce immutable instances of these objects.

To encode these use the [ilp-core-codecs](../ilp-core-codecs) module which offers codecs for encoding and decoding the objects according to the ASN.1 specifications and OER (Octet encoding rules).


### Interledger Address

Interledger Addresses can be created from a String and then manipulated using utility functions

```java
//Create a new address (short form)
InterledgerAddress address = InterledgerAddress.of("private.bob");

//Create a new address (long form)
InterledgerAddress destinationAddress = InterledgerAddress.builder()
  .value("private.bob")
  .build();

//Check if an address starts with a specific prefix
if( address.startsWith("test") ) {
  //Testnet address...
}

```

### Interledger Payment packet

```java

// Build ILP Payment Packet
InterledgerPreparePacket interledgerPreparePacket =
  InterledgerPreparePacket.builder()
    .destination(destination)
    .amount(amount)
    .executionCondition(condition)
    .expiresAt(expiry)
    .data(data)
    .build();
```
