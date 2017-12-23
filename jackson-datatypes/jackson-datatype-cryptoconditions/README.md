# Jackson Datatypes
This module contains support for serialiation and deserialization of Crypto Condition datatypes
for various encodings using the [Jackson Databind](https://github.com/FasterXML/jackson-databind/issues) 
framework. 

## Usage
This library is part of a larger framework for Jackson Databind integration, and can be used on its
own or as part of the broader [Quilt Jackson Databind](../README.md) framework.

However, to configure this module on its own, add the following maven coordinates to your project: 

```xml
<dependency>
  <groupId>org.interledger.quilt.jackson</groupId>
  <artifactId>jackson-datatype-cryptoconditions</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Default Encoding
To configure your ObjectMapper using default settings, add the following to your `ObjectMapper` 
configuration:

```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new CryptoConditionsModule());
```

By default, this module encodes each Condition using Base64 encoding. For example, a JSON payload 
using the default encoding might look like this:

```json
{
 "fulfillment": "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A="
}
```

### Alternate Encodings
This module supports 4 alternate encoding types, as defined in `CryptoConditionsModule.Encoding`.

#### Base64 Encoding (Without Padding)
To change the encoding to Base64 Encoding (without padding), initialize the CryptoConditionsModule 
as follows:

```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new CryptoConditionsModule(Encoding.BASE64_WITHOUT_PADDING));
```

#### Base64Url Encoding
To change the encoding to Base64Url Encoding, initialize the CryptoConditionsModule 
as follows:

```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new CryptoConditionsModule(Encoding.BASE64URL));
```


#### Base64Url Encoding (Without Padding)
To change the encoding to Base64 Encoding (without padding), initialize the CryptoConditionsModule 
as follows:

```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new CryptoConditionsModule(Encoding.BASE64URL_WITHOUT_PADDING));
```

#### HEX Encoding
To change the encoding to Hexadecimal, initialize the CryptoConditionsModule as follows:
```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new CryptoConditionsModule(Encoding.HEX));
```

Note that in order to use this encoding, you will need to include a dependency on Guava, as this 
module has only a `provided` dependency since most of the encoding settings don't utilize Guava. To 
include this dependency, add the following to your `pom.xml` file:

```xml
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <version>RELEASE</version>
    </dependency>
```
