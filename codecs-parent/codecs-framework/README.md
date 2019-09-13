# Hyperledger Quilt Codec Framework  [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/codec-framework.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Acodec-framework)

A framework for encoding and decoding POJOs into an ASN.1 defined data structure and then serializing/deserializing that ASN.1 object using Octet Encoding Rules (OER) or others.

## Codecs

The framework defines ASN.1 codecs for many basic ASN.1 types which can be extended easily
to add support for new objects.

All codecs implement `org.interledger.encoding.asn.framework.AsnObjectCodec<T>` where `T` is the type of object the codec is able to encode and decode.

For examples of how to use the framework for objects encoded as an ASN.1 SEQUENCE see the tests.

## Serializers

OER serializers are provided that can serialize all of the provided codecs or and new codecs that extend them.

## Usage

To serialize a custom object, create an `AsnObjectCodec` for it (extending one of the provided sub-classes of `AsnObjectCodecBase` if possible) and register this with the default `CodecContext` which can be instantiated via the `CodecContextFactory`.

The [ilp-core-codecs](../ilp-core-codecs) module offers codecs for encoding and decoding the core Interledger objects and can be used as an example.