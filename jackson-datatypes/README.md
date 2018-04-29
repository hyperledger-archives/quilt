# Jackson Datatypes
This module contains sub-modules to support serialiation and deserialization of common Quilt datatypes
to various encodings using the [Jackson Databind](https://github.com/FasterXML/jackson-databind/issues) 
framework. 

## Global Usage
To import _all_ of these modules into your project, you can simply add the following maven coordinates
to your project:

```xml
<dependency>
  <groupId>org.interledger.quilt.jackson</groupId>
  <artifactId>jackson-datatypes</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```

Next, configure your ObjectMapper with the following:

```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new HyperledgerQuiltModule());
```

## Individual Configurations
If you want to reduce the number of dependencies pulled into your project, you can alternatively choose 
from the following menu of libraries to add Jackson Databind support on a case-by-case basis.

### Crypto Conditions
To add support to Jackson for the [Crypto-Conditions](https://github.com/hyperledger/quilt/tree/master/crypto-conditions) 
classes, add the following maven coordinates to your project: 

```xml
<dependency>
  <groupId>org.interledger.quilt.jackson</groupId>
  <artifactId>jackson-datatype-cryptoconditions</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```

Next, configure your ObjectMapper with the following:

```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new CryptoConditionsModule());
```

### ILP Core
To add support to Jackson for the [ILP Core](https://github.com/hyperledger/quilt/tree/master/ilp-core) 
classes, add the following maven coordinates to your project: 

```xml
<dependency>
  <groupId>org.interledger.quilt.jackson</groupId>
  <artifactId>jackson-datatype-ilp-core</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```

Next, configure your ObjectMapper with the following:

```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new InterledgerCoreModule());
```