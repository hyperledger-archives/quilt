# Send Money over ILP using Quilt

This tutorial shows how to use the Quilt library to send money from one ILP account to another 
(via a [STREAM](https://interledger.org/rfcs/0029-stream/) payment) using the ILP Testnet. 
Complete working code for this tutorial can be found in [SendMoneyExample](./src/main/java/org/interledger/examples/SendMoneyExample.java)

## Testnet Account Setup

For this example, we need 2 different accounts on ILP Testnet, a sender and a receiver. You can create 
accounts using the **Generate XRP Credentials** button on https://xpring.io/ilp-testnet-creds

In this example, we will use the following account details:

Sender:
- Username: user_qb4yklwd
- Passkey: jxelaxvqz2ne6
- Payment Pointer: $rs3.xpring.dev/accounts/user_qb4yklwd/spsp
- Asset code: XRP _(note: this is actually XRP millidrops)_

Receiver:
- Username: user_jwmqq7kv
- Passkey: 58jehip9dktep
- Payment Pointer: $rs3.xpring.dev/accounts/user_jmchnh6k/spsp
- Asset code: XRP

## Sending STREAM Payment

High-level approach:
- Fetch the shared secret and destination address. We'll use Quilt's [SimpleSpspClient](../../spsp-parent/spsp-client/src/main/java/org/interledger/spsp/client/SimpleSpspClient.java)
since our Testnet account was created on an ILP node running  [Interledger.rs](https://github.com/interledger-rs/interledger-rs). 
- Create an [ILP over HTTP](https://interledger.org/rfcs/0035-ilp-over-http/) link using Quilt's 
[IlpOverHttpLink](../../link-parent/link-ilp-over-http/src/main/java/org/interledger/link/http/IlpOverHttpLink.java). 
- Create a STREAM connection using Quilt's [SimpleStreamSender](../../stream-parent/stream-client/src/main/java/org/interledger/stream/sender/SimpleStreamSender.java)
- Send money to the receiver account's payment pointer using the `sendMoney` method on `SimpleStreamSender`

## Code

The following constants are used in the code snippets. These should be replaced with your own account values:
```java
private static final String SENDER_ACCOUNT_USERNAME = "user_qb4yklwd";
private static final String SENDER_PASS_KEY = "jxelaxvqz2ne6";
private static final String RECEIVER_PAYMENT_POINTER = "$rs3.xpring.dev/accounts/user_jwmqq7kv/spsp";
private static final String TESTNET_URI = "https://rs3.xpring.dev";
```

First, we create an `SPSPClient`:
```java
SpspClient spspClient = new SimpleSpspClient();
```

Now we can fetch a SPSP response which contains the shared secret and destination address for sending payment:
```java
StreamConnectionDetails connectionDetails =
    spspClient.getStreamConnectionDetails(PaymentPointer.of(RECEIVER_PAYMENT_POINTER));
```

We can create a link using the following code:
```java
Link link = new IlpOverHttpLink(
    () -> SENDER_ADDRESS,
    HttpUrl.parse("https://example.com/accounts/peer/ilp"),
    newHttpClient(),
    new ObjectMapper(),
    InterledgerCodecContextFactory.oer(),
    new SimpleBearerTokenSupplier(SENDER_ACCOUNT_USERNAME + ":" + SENDER_PASS_KEY));
```

Using this `link`, we can now create a `SimpleStreamSender`:
```java
SimpleStreamSender simpleStreamSender = new SimpleStreamSender(link);
```

Now, we can send a payment for 1000 millidrops from our source account to our destination account using the shared secret.
```java

SendMoneyRequest sendMoneyRequest = SendMoneyRequest.builder()
      .sourceAddress(InterledgerAddress.of(SPSP_SENDER))
      .amount(UnsignedLong.valueOf(1000L))
      .denomination(Denomination.builder().assetScale((short) 9).assetCode(XRP).build())
      .destinationAddress(connectionDetails.destinationAddress())
      .sharedSecret(connectionDetails.sharedSecret())
      .build();

SendMoneyResult result = simpleStreamSender.sendMoney(sendMoneyRequest).get();
```

Finally, we can verify the account balance of the receiver using the Rust admin client:
```java
InterledgerRustNodeClient rustClient =
    new InterledgerRustNodeClient(newHttpClient(), SENDER_ACCOUNT_USERNAME + ":" + SENDER_PASS_KEY, TESTNET_URI);
System.out.println("Ending balance for sender: " + rustClient.getBalance(SENDER_ACCOUNT_USERNAME));
``` 

