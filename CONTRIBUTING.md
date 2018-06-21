# Contributing

Hyperledger Quilt is a truly open source project. It is a community led effort to provide the components developers need to work with the [Interledger Protocol](https://interledger.org).

The initial versions of Quilt are *_Java_* implementations however any other language implementation is welcomed. Please log an issue if you'd like to contribute using a new lnaguage/stack and we'll get the necessary repo changes made so you can.

First time contributors can either find a module that has not yet been implemented and try that or look for a ["Good First Issue"]() 

## Goals

To use Interledger an application requires one or more of the following components:

  1. Interledger Protocol Codecs
  1. ILDCP Protocol Codecs
  1. Bilateral Transfer Protocol Codecs
  1. STREAM Protocol Codecs, Sender and Receiver

Our goal is to provide these componenets for as many languages/platforms as possible.

## Structure and Conventions

For each protocol in the stack there is a module that defines the domain objects and another module that provides the codecs for those objects, leveraging the codec framework. New protocols should follow the same convention.

Domain objects are defined as interfaces with internal abstract class definitions that use the Immutables library to generate immutable implementations with handy builders.

The standard Immutables config is encapsulated in a custom annotation which should be used on the abstract class definitions. See the interfaces in [ilp-core](ilp-core) as an example.

## Code Style

We are using checkstyle to enforce our style rules which are defined in the [dev-ops](dev-ops) project.

## Codec Framework

See: [codec-framework](codec-framework)

All ILP protocols use Octet Encoding Rules for encoding data on the wire. The message definitions are in ASN.1 notation and the Quilt project provides language native domain objects for these.

The library also provides a framework that can be used to define ASN.1 representations of the domain objects and serializers to encode/decode the objects on/off the wire.

If there are any generic ASN.1 types that are not provided in the codec framework, these should be implemented as required in the framework. Protocol specific types are implemented in their own sub-modules. 

### Interledger Protocol

See: [ilp-core](ilp-core)

This project defines the doamin objects required for the core protocol.
Codecs are in [ilp-core-codecs](ilp-core-codecs).

### Interledger Dynamip Configuration Protocol

See: [ildcp-core](ildcp-core)

This project defines the domain objects required for the IL-DCP messages exchange inside ILP packets between peers using the IL-DCP protocol.
Codecs are in [ildcp-core-codecs](ildcp-core-codecs).

### Bilateral Transfer Protocol

See: [btp-core](btp-core)

BTP is a protocol commonly used between peers to carry ILP packets and also negotiate reconciliation and settlement.
It is most commonly used over Websockets but any transport that is able to frame messages would work.

### STREAM Protocol

STREAM is the transport protocol that is used to establish a connection between two enitities on the Interledger and exchange money and data between them.
No implemetation is available in Java yet, *THIS IS A GREAT, BUT ADVANCED, COPNTRIBUTION TO TACKLE*.
