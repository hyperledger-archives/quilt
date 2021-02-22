# Send Money over ILP using Quilt

This tutorial shows how to use the Quilt library to send money from one ILP account to another 
(via a [STREAM](https://interledger.org/rfcs/0029-stream/) payment) using the ILP Testnet. 
Complete working code for this tutorial can be found in [SendMoneyExample](./src/main/java/org/interledger/examples/SendMoneyExample.java) 
(if you want to use the now-deprecated SimpleStreamSender) or in 
[SendMoneyWithIlpPayExample](./src/main/java/org/interledger/examples/SendMoneyWithIlpPayExample.java) (to see how payments can be made with 
the new ILP Pay system).

## Testnet Account Setup

For this example, we need 2 different accounts on ILP Testnet, a sender and a receiver. You can create 
accounts using the **Generate XRP Credentials** button on [https://ripplex.io/](https://ripplex.io/).

In this example, we will use the following account details (*these should be replaced with your own account values*):

**Sender:**
- Username: `demo_user`
- Passkey: `OWIyMzUwMzgtNWUzZi00MDU1LWJlMmUtZjk4NjdmMTJlOWYz`
- Payment Pointer: `$ripplex.money/demo_user`
- Asset code: `XRP` _(note: this is actually XRP milli-drops)_

**Receiver:**
- Username: `demo_receiver`
- Passkey: `MjI5ODUxZWMtYzA4OC00ZjBkLWI2ZjktOTA4MmYxNGZiODAw`
- Payment Pointer: `$ripplex.money/demo_receiver`
- Asset code: `XRP` _(note: this is actually XRP milli-drops)_

**Constants:**
```java
private static final String SENDER_ACCOUNT_USERNAME = "demo_user";
private static final String SENDER_PASS_KEY = "...";
private static final String RECEIVER_PAYMENT_POINTER = "$ripplex.money/demo_receiver";
private static final String TESTNET_URI = "https://rxprod.wc.wallet.ripplex.io";
```

## Send a STREAM Payment (ILP-Pay)

The following is an outline of the high-level approach (see actual code below):
- Initialize all required constants and classes.
- Obtain a `Quote` from the payment path using a `PaymentOptions` that contains all details about the payment about to be made.
- Use the `Quote` to make the payment, yielding a `PaymentReceipt` with information about the payment.

### Code Walk through

First, we create a `StreamPacketEncryptionService` using the default AES encryptor (`AesGcmStreamSharedSecretCrypto`):
```java
StreamPacketEncryptionService streamPacketEncryptionService = new StreamPacketEncryptionService(
  StreamCodecContextFactory.oer(), new AesGcmStreamSharedSecretCrypto()
);
```

Next, we create an ILP-over-HTTP Link using the credentials defined above (don't forget to set the `LinkId`):
```java
 Link<?> link = newIlpOverHttpLink(TESTNET_URI, SENDER_AUTH_TOKEN);
 link.setLinkId(LinkId.of("ILP Drip Source Account"));
```

Next, we construct the actual `StreamPayer` using an external exchange-rate service that returns faux-rates. In this
case, all identity FX rates will be `1:1`, and USD-to-XRP rates will be `0.25`. This examples makes an XRP-to-XRP payment,
so the FX rate will be 1:1 (i.e., `1.0`).

```java
StreamPayer streamPayer = new StreamPayer.Default(
  streamPacketEncryptionService,
  link,
  new DefaultOracleExchangeRateService(new FauxRateProvider(new BigDecimal("0.25"))
);
```

Now that everything is initialized, we construct an instance of `PaymentOptions`, which contains all inputs 
needed by the software to attempt to make a payment:

```java
PaymentOptions paymentOptions = PaymentOptions.builder()
  .senderAccountDetails(AccountDetails.builder()
    .denomination(Denominations.XRP_MILLI_DROPS)
    .interledgerAddress(OPERATOR_ADDRESS)
    .build())
  .amountToSend(BigDecimal.valueOf(1.50)) // <-- Send 1.5 XRP.
  .destinationPaymentPointer(RECEIVER_PAYMENT_POINTER)
  .paymentTimeout(Duration.ofSeconds(30)) // <-- Don't wait more than 30s, whether success or failure.
  .slippage(Slippage.of(Percentage.of(new BigDecimal("0.10")))) // <-- Allow up to 10% slippage.
  .build();
```

Next, let's obtain a `Quote`, which tells us the current conditions of the payment path, and also yields some estimated
outcomes that we can consider before we make our payment:

```java
Quote quote = streamPayer.getQuote(paymentOptions)
  .handle((q, throwable) -> {
    if (throwable != null) {
      LOGGER.error(throwable.getMessage(), throwable); // <-- This is optional, but helpful if something goes wrong.
      return null;
    }
    return q;
  })
  .join();
```

Last but not least, we can initiate a payment using the `Quote`, like this:

```java
PaymentReceipt paymentReceipt = streamPayer.pay(quote)
  .handle((pr, throwable) -> {
    if (throwable != null) {
      LOGGER.error(throwable.getMessage(), throwable); // <-- This is optional, but helpful if something goes wrong.
      return null;
    } else {
      return pr;
    }
  }).join();
```

Finally,we can verify the account balance using curl:

```bash
curl --location --request GET 'https://rxprod.wc.wallet.ripplex.io/accounts/demo_receiver/balance' \
--header 'Accept: application/json' \
--header 'Authorization: MjI5ODUxZWMtYzA4OC00ZjBkLWI2ZjktOTA4MmYxNGZiODAw'
``` 

## Send a STREAM Payment (SimpleStreamSender)
*Note: This approach has been deprecated in-favor of ILP Pay functionality.*

The following is an outline of our high-level approach (see actual code below):

- Fetch the shared secret and destination address. We'll use Quilt's [SimpleSpspClient](../../spsp-parent/spsp-client/src/main/java/org/interledger/spsp/client/SimpleSpspClient.java)
because our Testnet account was created on an ILP node running [Interledger4j](https://connector.interledger4j.dev). 
- Create an [ILP over HTTP](https://interledger.org/rfcs/0035-ilp-over-http/) link using Quilt's 
[IlpOverHttpLink](../../link-parent/link-ilp-over-http/src/main/java/org/interledger/link/http/IlpOverHttpLink.java). 
- Create a STREAM connection using Quilt's [SimpleStreamSender](../../stream-parent/stream-client/src/main/java/org/interledger/stream/sender/SimpleStreamSender.java)
- Send money to the receiver account's payment pointer using the `sendMoney` method on `SimpleStreamSender`


### Code Walk through

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
  HttpUrl.parse("https://rxprod.wc.wallet.ripplex.io/accounts/demo_user/ilp"),
  newHttpClient(),
  new ObjectMapper(),
  InterledgerCodecContextFactory.oer(),
  new SimpleBearerTokenSupplier(SENDER_ACCOUNT_USERNAME + ":" + SENDER_PASS_KEY));
```

Using this `link`, we can now create a `SimpleStreamSender`:
```java
SimpleStreamSender simpleStreamSender = new SimpleStreamSender(link);
```

Now, we can send a payment for 1000 milli-drops from our source account to our destination account using the shared secret.
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

Finally,we can verify the account balance using curl:

```bash
curl --location --request GET 'https://rxprod.wc.wallet.ripplex.io/accounts/demo_receiver/balance' \
--header 'Accept: application/json' \
--header 'Authorization: MjI5ODUxZWMtYzA4OC00ZjBkLWI2ZjktOTA4MmYxNGZiODAw'
```
