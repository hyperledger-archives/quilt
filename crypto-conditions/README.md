# Crypto-Conditions [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/crypto-conditions.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Acrypto-conditions)

Java implementation of Crypto-Conditions (See [RFC](https://datatracker.ietf.org/doc/draft-thomas-crypto-conditions/)).

* v0.3.x-SNAPSHOT (and above) implements the latest RFC [draft-03](https://tools.ietf.org/html/draft-thomas-crypto-conditions-03).
* v0.2.x-SNAPSHOT implements the latest RFC [draft-02](https://tools.ietf.org/html/draft-thomas-crypto-conditions-02).  

## Dependencies

This library uses various cryptographic functions so it relies on implementations of RSA and ED25519 signature schemes.

For RSA any provider that supports **SHA256withRSA/PSS** signatures can be used. The library has been tested with BouncyCastle v1.46 but has no runtime dependancy on it.

For ED25519 the library depends on [net.i2p.crypto.eddsa](https://github.com/str4d/ed25519-java). As there are no standard interfaces in the `java.security` namespace for EdDSA keys the library is included as a dependancy. Future versions will hopefully remove this dependency.

## Get it!

### Maven
This library is contained in the Java package `org.interledger.cryptoconditions`, and can be included in your project 
by first adding a Snapshot Repository, like this:

```
<repositories>
    ...
    <repository>
        <id>sonatype</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    </repository>
    ...
</repositories>
```

Next, add the following Maven dependency:

```
<dependencies>
  ...
  <dependency>
    <groupId>org.interledger</groupId>
    <artifactId>crypto-conditions</artifactId>
    <version>0.4.0-SNAPSHOT</version>
  </dependency>
  ...
</dependencies>
```
### Gradle
To import this library into a project that uses gradle, first add the Snapshot Repository to your `gradle.properties` file, like this:

```
repositories {
    mavenCentral()
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}
```
Next, import this library as a dependency, like this:

```
dependencies {
    ...
    compile group: 'org.interledger', name: 'crypto-conditions', version: '0.4.0-SNAPSHOT'
    ...
}
```

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
  byte[] preimage = "My Secret Preimage".getBytes(Charset.defaultCharset());

  PreimageSha256Fulfillment fulfillment = PreimageSha256Fulfillment.from(preimage);
  PreimageSha256Condition condition = fulfillment.getDerivedCondition();

  if (fulfillment.verify(condition, new byte[0])) {
    System.out.println("Fulfillment is valid!");
  }
```

#### PREFIX-SHA-256 Example:
```java
  // Create a sub-fulfillment...
	final byte[] preimage = "My Secret Preimage".getBytes(Charset.defaultCharset());
	PreimageSha256Fulfillment subfulfillment = PreimageSha256Fulfillment.from(preimage);
  
	// Narrow the subfulfillment with a prefix...
	final String prefix = "order-1234";
	final PrefixSha256Fulfillment fulfillment = PrefixSha256Fulfillment
	    .from(prefix.getBytes(), 100, subfulfillment);
	final PrefixSha256Condition condition = fulfillment.getDerivedCondition();
  
	// Verify the fulfillment
	if (fulfillment.verify(condition, new byte[0])) {
	  System.out.println("Fulfillment is valid!");
	}
```

#### ED25519-SHA-256 Example
```java
  // An optional message to sign...should be "new byte[0]" if no message.
  byte[] optionalMessageToSign = "message".getBytes();

  //Generate ED25519-SHA-256 KeyPair and Signer
  MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");
  net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
  KeyPair edDsaKeyPair = edDsaKpg.generateKeyPair();
  Signature edDsaSigner = new EdDSAEngine(sha512Digest);
  
  edDsaSigner.initSign(edDsaKeyPair.getPrivate());
  edDsaSigner.update(optionalMessageToSign);
  byte[] edDsaSignature = edDsaSigner.sign();
  
  //Generate ED25519-SHA-256 Fulfillment and Condition
  Ed25519Sha256Fulfillment fulfillment = Ed25519Sha256Fulfillment.from(
  (EdDSAPublicKey) edDsaKeyPair.getPublic(), edDsaSignature);
  Ed25519Sha256Condition condition = fulfillment.getDerivedCondition();
  
  if (fulfillment.verify(condition, optionalMessageToSign)) {
    System.out.println("Fulfillment is valid!");
  }
```

#### RSA-SHA-256 Example
```java
  // An optional message to sign...should be "new byte[0]" if no message.
  final byte[] optionalMessageToSign = "message".getBytes(); 
  
  //Generate RSA-SHA-256 KeyPair and Signer
  final KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
  rsaKpg.initialize(new RSAKeyGenParameterSpec(2048, new BigInteger("65537")));
  final KeyPair rsaKeyPair = rsaKpg.generateKeyPair();
  final RSAPublicKey rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic()
  
  final Signature rsaSigner = Signature.getInstance("SHA256withRSA/PSS");
  rsaSigner.initSign(rsaKeyPair.getPrivate());
  rsaSigner.update(optionalMessageToSign);
  final byte[] rsaSignature = rsaSigner.sign();
  
  final RsaSha256Fulfillment fulfillment = RsaSha256Fulfillment.from(rsaPublicKey, rsaSignature);
  final RsaSha256Condition condition = RsaSha256Condition.from(rsaPublicKey);
  
  if (fulfillment.verify(condition, optionalMessageToSign)) {
    System.out.println("Fulfillment is valid!");
  }
```

#### THRESHOLD-SHA-256 Example
```java
//Generate PreimageSha256Condition Number 1
RsaSha256Condition rsaCondition = PreSha256Condition.from(rsaPublicKey);

byte[] message = new byte[0];
RsaSha256Condition rsaFulfillment = RsaSha256Condition.from(rsaPublicKey, message);

//Generate ED25519-SHA-256 condition
net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
KeyPair edDsaKeyPair = edDsaKpg.generateKeyPair();
Signature edDsaSigner = new EdDSAEngine(sha512Digest);

PreimageSha256Fulfillment fulfillment = PreimageSha256Fulfillment.from(preimage);
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

[![Rocket.Chat](https://open.rocket.chat/images/join-chat.svg)](https://chat.hyperledger.org/channel/quilt)

## License

This code is released under the Apache 2.0 License. Please see [LICENSE](./../LICENSE) for the full text.