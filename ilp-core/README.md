# Interledger Core (Java) [![join the chat on gitter][gitter-image]][gitter-url] [![circle-ci][circle-image]][circle-url] [![codecov][codecov-image]][codecov-url]

[gitter-url]: https://gitter.im/interledger/java
[gitter-image]: https://badges.gitter.im/interledger/java.svg
[circle-image]: https://circleci.com/gh/interledger/java-ilp-core.svg?style=shield
[circle-url]: https://circleci.com/gh/interledger/java-ilp-core
[codecov-image]: https://codecov.io/gh/interledger/java-ilp-core/branch/master/graph/badge.svg
[codecov-url]: https://codecov.io/gh/interledger/java-ilp-core


Base library for Interledger projects providing service interfaces, event descriptions, exceptions and data models.

For more information about Interledger specifications that underpin this library, please reference [https://github.com/interledger/rfcs](https://github.com/interledger/rfcs).

## Usage
For more detail about how to use this library, consult the [wiki](https://github.com/interledger/java-ilp-core/wiki)

The library offers interfaces for all core objects and default builders that produce immutable instances of these objects.

The library also offers a codecs for encoding and decoding the objects according to the ASN.1 specifications and OER (Octet encoding rules).


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

### Conditions and Fulfillments

Conditions can be derived from a fullfillment or directly from the underlying hash

```java
//Create a fulfillment from a pre-image of 32-bytes (short form)
Fulfillment fulfillment = Fulfillment.of(preimage);

//Create a fulfillment from a pre-image of 32-bytes (long form)
Fulfillment fulfillment = Fulfillment.builder()
  .preimage(preimage)
  .build();

//Get Condition from Fulfillment
Condition condition = fulfillment.getCondition();

//Create Condition (short form)
Condition condition1 = Condition.of(hash);

//Create Condition (long form)
Condition condition1 = Condition.builder()
  .hash(hash)
  .build();
```

### Interledger Payment packet

```java

// Build ILP Payment Packet
InterledgerPayment payment = InterledgerPayment.builder()
  .destinationAccount(address)
  .destinationAmount(destinationAmount)
  .data(data)
  .build();
```

### Interledger Payment Request

```java

InterledgerPaymentRequest ipr = InterledgerPaymentRequest.builder()
  .payment(payment)
  .condition(condition)
  .build();
```

### Pre-Shared Key protocol

The PSK protocol defines an envelope format for the data in an ILP Payment packet but also a number
of algorithms for deriving various keys and secrets based on a single receiver secret.

The PSK Context is created using the receiver secret (on the receiver side) or just the shared key
on the sender side. A sender side context cannot be used to derive a receiverId and has no random
token so calling either `getToken()` or `getReceiverId()` will throw.

See [ILP-RFC 16](https://interledger.org/rfcs/0016-pre-shared-key/) for more details of PSK

```java
// Create a new context at the receiver and seed with a new random token
PskContext context = PskContext.seed(SECRET);

//Load a context from an incoming payment using the token extracted from the destination address
in the payment
PskContext receiverContext =
    PskContext.fromReceiverAddress(SECRET, incomingPayment.getDestinationAccount());

//Load a context from the shared key provided to the sender during the SPSP exchange
PskContext senderPskContext = PskContext.fromPreSharedKey(psk);

//Create a PSK Message
PskMessage pskMessage = PskMessage.builder()
  .paymentId(paymentId)
  .expiry(expiry)
  .addPrivateHeader("Secret", secretStuff)
  .data(data)
  .build();

// Encrypt message
PskMessage encryptedPskMessage = context.encryptMessage(pskMessage);

// Decrypt message
PskMessage decryptedPskMessage = context.decryptMessage(encryptedPskMessage);

```

For a full end-to-end test demonstrating all of these functions see:
`org.interledger.ipr.InterledgerPaymentRequestEndToEndTest`

## Development
We welcome any and all submissions, whether it's a typo, bug fix, or new feature.

### Requirements
This project uses Gradle to manage dependencies and other aspects of the build.  To install Gradle, follow the instructions at [https://gradle.org](https://gradle.org/).

### Get the Code

```bash
$ git clone https://github.com/interledger/java-ilp-core.git
```

### Build the Project
To build the project, execute the following command from the top-level folder that you cloned the above two projects to.  For example:

```bash
$ gradle build test
```

#### Checkstyle
The project uses checkstyle to keep code style consistent. To run the style checks:

```bash
$ gradle build check
```

### Contributing
This project utilizes a Pull Request submission model.  Before submitting a pull request, ensure that your build passes with no test failures nor Checkstyle errors.