# Crypto-Conditions [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/crypto-conditions.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Acrypto-conditions)

Java implementation of Crypto-Conditions (See [RFC](https://datatracker.ietf.org/doc/draft-thomas-crypto-conditions/)).

* v0.3.x-SNAPSHOT (and above) implements the latest RFC [draft-03](https://tools.ietf.org/html/draft-thomas-crypto-conditions-03).
* v0.2.x-SNAPSHOT implements the latest RFC [draft-02](https://tools.ietf.org/html/draft-thomas-crypto-conditions-02).  

## Dependencies

This library uses various cryptographic functions so it relies on implementations of RSA and ED25519 signature schemes.

For RSA any provider that supports **SHA256withRSA/PSS** signatures can be used. The library has been tested with BouncyCastle v1.46 but has no runtime dependancy on it.

For ED25519 the library depends on [net.i2p.crypto.eddsa](https://github.com/str4d/ed25519-java). As there are no standard interfaces in the `java.security` namespace for EdDSA keys the library is included as a dependancy. Future versions will hopefully remove this dependency.

 
## Usage

### Requirements
This project uses Maven to manage dependencies and other aspects of the build. 
To install Maven, follow the instructions at [https://maven.apache.org/install.html](https://maven.apache.org/install.html).


### Get the code

``` sh
git clone https://github.com/hyperledger/quilt
cd quilt/crypto-conditions
```

### Build the Project
To build the project, execute the following command:

```bash
$ mvn clean install
```

#### Checkstyle
The project uses checkstyle to keep code style consistent. All Checkstyle checks are run by default during the build, but if you would like to run checkstyle checks, use the following command:

```bash
$ mvn checkstyle:checkstyle
```

### Step 3: Use

#### PREIMAGE-SHA-256 Example:
```java
byte[] preimage = "Hello World!".getBytes(Charset.defaultCharset());
PreimageSha256Condition condition = new PreimageSha256Condition(preimage);

PreimageSha256Fulfillment fulfillment = new PreimageSha256Fulfillment(preimage);
if(fulfillment.validate(condition)) {
    System.out.println("Fulfillment is valid!");
}
```

#### THRESHOLD-SHA-256, ED25519-SHA-256 and RSA-SHA-256 Example:
```java
//Generate RSA-SHA-256 condition
KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
rsaKpg.initialize(new RSAKeyGenParameterSpec(2048, new BigInteger("65537")));
KeyPair rsaKeyPair = rsaKpg.generateKeyPair();

RsaSha256Condition condition = new RsaSha256Condition((RSAPublicKey) rsaKeyPair.getPublic());

//Generate ED25519-SHA-256 condition
net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
KeyPair edDsaKeyPair = edDsaKpg.generateKeyPair();
Signature edDsaSigner = new EdDSAEngine(sha512Digest);

PreimageSha256Fulfillment fulfillment = new PreimageSha256Fulfillment(preimage);
//Verify against empty message
if(fulfillment.verify(condition, new byte[0])) {
    System.out.println("Fulfillment is valid!");
}
```


#### Encoding Example:
```java
//Read a condition from a stream (InputStream in)
DERInputStream derStream = new DERInputStream(in);
Condition condition = CryptoConditionReader.readCondition(derStream);

//Read a fulfillment from a stream (InputStream in)
DERInputStream derStream = new DERInputStream(in);
Fulfillment fulfillment = CryptoConditionReader.readFulfillment(derStream);

//Read a condition from a byte array (byte[] buffer)
Condition condition = CryptoConditionReader.readCondition(buffer);

//Read a fulfillment from a byte array (byte[] buffer)
Fulfillment fulfillment = CryptoConditionReader.readFulfillment(buffer);

//Get binary encoding of condition that can be written to stream
byte[] binaryEncodedCondition = CryptoConditionWriter.writeCondition(condition);

//Get binary encoding of fulfillment that can be written to stream
byte[] binaryEncodedCondition = CryptoConditionWriter.writeFulfillment(fulfillment);

//Get ni: URI form for sharing via text-based protocols
URI uriEncodedCondition = CryptoConditionUri.toUri(condition);
```

## Contributors

Any contribution is very much appreciated! 

[![join the chat][rocketchat-image]][rocketchat-url]

## TODO

  - Replace current ASN.1 DER Input/Outputstream code with Codec framework (see java-ilp-core).
  - Validate condition against a global max cost

## License

This code is released under the Apache 2.0 License. Please see [LICENSE](./../LICENSE) for the full text.
