# Interledger Core Codecs [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-core-codecs.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-core-codecs)

Codecs framework for encoding and decoding the core Interledger classes using the Octet Encoding Rules (OER).

## Usage

To serialize a an object you will need a `CodecContext`. The provided factory will create one of these with the base types and Interledger types registered.

Example:

```java
//This can be done once and used multiple times.
final CodecContext context = InterledgerCodecContextFactory.oer();

//Read an object from a stream
final InterledgerPreparePacket payment 
  = context.read(InterledgerPreparePacket.class, inputStream);

//Write an object to a stream
final InterledgerPreparePacket packet 
  = InterledgerPreparePacket.builder()
  .destination(InterledgerAddress.of("test3.foo"))
  .amount(BigInteger.valueOf(100L))
  .executionCondition(
      PreimageSha256Condition.fromCostAndFingerprint(32, preimage))
  .expiresAt(Instant.now().plus().plusSeconds(30))
  .build();

context.write(packet, outputStream);
```
