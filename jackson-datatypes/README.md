# Jackson Datatypes
This module contains sub-modules to support serialization and deserialization of common Quilt datatypes
to various encodings using the [Jackson Databind](https://github.com/FasterXML/jackson-databind/issues) 
framework. 

## Global Usage
To import _all_ of these modules into your project, you can simply add the following maven coordinates
to your project:

```xml
<dependency>
  <groupId>org.interledger.quilt.jackson</groupId>
  <artifactId>jackson-datatypes</artifactId>
  <version>0.17.0-SNAPSHOT</version>
</dependency>
```

Next, configure your ObjectMapper with the following:

```java
  ObjectMapper objectMapper = new ObjectMapper()
  // ... other Module registrations
  .registerModule(new HyperledgerQuiltModule());
```