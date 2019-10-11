# Hyperledger Quilt [![Discuss][forum-image]][forum-url] [![twitter][twitter-image]][twitter-url]
[![circle-ci][circle-image]][circle-url] [![codecov][codecov-image]][codecov-url] [![codacy][codacy-image]][codacy-url] [![issues][github-issues-image]][github-issues-url]

Quilt is a Java implementation of the [Interledger](https://interledger.org) protocol. 

This library can be used to send and receive Interledger payments using STREAM. It also supports a variety of other core Interledger primitives and protocols.

Note that this library does not implement a full Java Connector. For this functionality, see the [Java ILP Connector](https://github.com/sappenin/java-ilpv4-connector) project, which is built using Quilt. 

## Modules

The quilt project is organised as a Maven multi-module project. Each module exists in a subdirectory that has its own child POM and README files.

Dependency and plugin versions are managed in the parent project.

Issues are labelled and prefixed to make it easy to identify which project they relate to.

### ilp-core

The `ilp-core` module is the base library for any Interledger Java projects, providing service interfaces, packet definitions, and data models supporting the ILPv4 protocol (defined in [IL-RFC-27](https://github.com/interledger/rfcs/blob/master/0027-interledger-protocol-4/0027-interledger-protocol-4.md)). 

These primitives form the foundation of the Interledger suite of protocols, and are used throughout the other modules in this project.

[![Javadocs](https://www.javadoc.io/badge/org.interledger/ilp-core.svg?&label=javadoc ilp-core)](https://www.javadoc.io/doc/org.interledger/ilp-core) [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-core.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-core) 

[READ MORE](./ilp-core)

### btp-core
The `btp-core` module contains primitives to support the Bilateral Transfer Protocol (BTP v2.0) as defined by [IL-RFC-23](https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md).

[![Javadocs](https://www.javadoc.io/badge/org.interledger/btp-core.svg?label=javadoc btp-core)](https://www.javadoc.io/doc/org.interledger/btp-core) [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/btp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Abtp)

### codecs-parent
The `codecs-parent` module contains an encoding and decoding framework plus serializers for ASN.1 OER formats defined in each IL-RFC. This module supports all primitives for the following protocols: [ILP v4.0](https://github.com/interledger/rfcs/blob/master/0027-interledger-protocol-4/0027-interledger-protocol-4.md), [IL-DCP v1.0](https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md), [BTP 2.0](https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md), and [STREAM v1.0](https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md).

[![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-btp.svg?label=javadoc%3Acodecs-framework)](https://www.javadoc.io/doc/org.interledger/codecs-framework)
[![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-btp.svg?label=javadoc%3Acodecs-btp)](https://www.javadoc.io/doc/org.interledger/codecs-btp)
[![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-btp.svg?label=javadoc%3Acodecs-ilp)](https://www.javadoc.io/doc/org.interledger/codecs-ilp)
[![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-btp.svg?label=javadoc%3Acodecs-ildcp)](https://www.javadoc.io/doc/org.interledger/codecs-ildcp)
[![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-btp.svg?label=javadoc%3Acodecs-stream)](https://www.javadoc.io/doc/org.interledger/codecs-stream)

### ildcp-core
The `ildcp-core` module contains primitives to support the Interledger Dynamic Configuration Protocol (IL-DCP v1.0) as defined by [IL-RFC-31](https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md).

[![Javadocs](https://www.javadoc.io/badge/org.interledger/ildcp-core.svg?label=javadoc ildcp-core)](https://www.javadoc.io/doc/org.interledger/ildcp-core) [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ildcp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Aildcp)

### jackson-datatypes
The `jackson-datatypes` module contains utilities to marshal and unmarshal various Quilt primitives to and from JSON using the [Jackson library](https://github.com/FasterXML/jackson).

[![Javadocs](https://www.javadoc.io/badge/org.interledger/jackson-datatypes.svg?label=javadoc jackson-datatypes)](https://www.javadoc.io/doc/org.interledger/jackson-datatypes) [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/jackson.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ajackson)

### link-parent
The `link-parent` module contains libraries that can be used as a network transport for Interledger accounts. Currently supported links are ILP-over-HTTP as defined in [IL-RFC-35](https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md).

[![Javadocs](https://www.javadoc.io/badge/org.interledger/link-core.svg?label=javadoc link-core)](https://www.javadoc.io/doc/org.interledger/link-core) [![Javadocs](https://www.javadoc.io/badge/org.interledger/link-ilp-over-http.svg?label=javadoc link-ilp-over-http)](https://www.javadoc.io/doc/org.interledger/link-ilp-over-http) [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-link.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-link)

### spsp-parent
The `spsp-parent` module contains libraries that can be used to resolve [Payment Pointers](https://paymentpointers.org/) (as defined in [IL-RFC-26](https://github.com/interledger/rfcs/blob/master/0026-payment-pointers/0026-payment-pointers.md) as well as the broader Simple Payment Setup Protocol (SPSP) as defined in [IL-RFC-009](https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md).

[![Javadocs](https://www.javadoc.io/badge/org.interledger/spsp-core.svg?label=javadoc spsp-core)](https://www.javadoc.io/doc/org.interledger/spsp-core) [![Javadocs](https://www.javadoc.io/badge/org.interledger/spsp-client.svg?label=javadoc spsp-client)](https://www.javadoc.io/doc/org.interledger/spsp-client) [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/spsp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Aspsp)

### stream-parent
The `stream-parent` module contains libraries for sending and receiving packetized payments using the STREAM protocol, defined in [IL-RFC-29](https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md).

[![Javadocs](https://www.javadoc.io/badge/org.interledger/spsp-client.svg?label=javadoc stream-sender)](https://www.javadoc.io/doc/org.interledger/stream-client) [![Javadocs](https://www.javadoc.io/badge/org.interledger/spsp-client.svg?label=javadoc spsp-core)](https://www.javadoc.io/doc/org.interledger/streamc-core) [![Javadocs](https://www.javadoc.io/badge/org.interledger/spsp-client.svg?label=javadoc spsp-receiver)](https://www.javadoc.io/doc/org.interledger/streamc-receiver) [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/stream.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Astream)

### examples
The `examples` modules shows how to use the Quilt library in your code.

[READ MORE](./examples-parent)

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/examples.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Aexamples)

## Usage

### Requirements

#### Unlimited Strength Encryption Policy Files 
In order to properly build this project, you must download and install Java Cryptography Extension 
(JCE) Unlimited Strength Jurisdiction Policy files. For more details, follow the instructions 
[here](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html). 

### Maven
Modules in this library can be included in your Maven project by adding the Maven dependency for the module you would like to use. For example, to import `ilp-core`, use the following coordinates:

```
<dependencies>
  ...
  <dependency>
    <groupId>org.interledger</groupId>
    <artifactId>ilp-core</artifactId>
    <version>1.0.0</version>
  </dependency>
  ...
</dependencies>
```

### Gradle
Modules in this library can be included in your Gradle project by adding the Maven dependency for the module you would like to use. For example, to import `ilp-core`, use the following coordinates:

```
dependencies {
    ...
    compile group: 'org.interledger', name: 'ilp-core', version: '1.0.0'
    ...
}
```

### Artifacts
Artifacts for this project are published to Maven Central. For a complete list, see: [https://search.maven.org/search?q=g:org.interledger](https://search.maven.org/search?q=g:org.interledger).

## Development
We welcome any and all submissions, whether it's a typo, bug fix, or new feature. To get started, first download the code:

``` sh
git clone https://github.com/hyperledger/quilt
cd quilt
```

### Maven
This project uses Maven to manage dependencies and other aspects of the build. 
To install Maven, follow the instructions at [https://maven.apache.org/install.html](https://maven.apache.org/install.html).

Snapshot dependencies for this library can be included in your project by first adding a Snapshot Repository to your `pom.xml` file, like this:

```
<repositories>
    ...
    <snapshotRepository>
        <id>sonatype</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    </snapshotRepository>
    ...
</repositories>
```

Next, add the following Maven dependency:

```
<dependencies>
  ...
  <dependency>
    <groupId>org.interledger</groupId>
    <artifactId>ilp-core</artifactId>
    <version>HEAD-SNAPSHOT</version>
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
    compile group: 'org.interledger', name: 'ilp-core', version: 'HEAD-SNAPSHOT'
    ...
}
```

### Docker
By default, the build runs integration tests which depend on Docker daemon to be running. If you don't have Docker running, you can
skip integration tests using `-DskipITs` (e.g. `mvn clean install -DskipITs`).

Otherwise, to install docker, follow the instructions at [https://docs.docker.com/install/](https://docs.docker.com/install/) 


### Build the Project
To build the project, execute the following command:

```bash
$ mvn clean install
```

### Checkstyle
The project uses checkstyle to keep code style consistent. All Checkstyle checks are run by default during the build, but if you would like to run checkstyle checks, use the following command:

```bash
$ mvn checkstyle:checkstyle
```

[forum-url]: https://forum.interledger.org/tags/java
[forum-image]: https://img.shields.io/badge/Interledger%20Forum-java-blue.svg
[circle-image]: https://circleci.com/gh/hyperledger/quilt.svg?style=shield
[circle-url]: https://circleci.com/gh/hyperledger/quilt
[codecov-image]: https://codecov.io/gh/hyperledger/quilt/branch/master/graph/badge.svg
[codecov-url]: https://codecov.io/gh/hyperledger/quilt
[codacy-image]: https://api.codacy.com/project/badge/Grade/02e8d6c0d9d8482e9e8d9725ceb64d9f
[codacy-url]: https://www.codacy.com/app/sappenin/quilt?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=hyperledger/quilt&amp;utm_campaign=Badge_Grade
[twitter-image]: https://img.shields.io/twitter/follow/interledger.svg?style=social
[twitter-url]: https://twitter.com/intent/follow?screen_name=interledger
[github-issues-image]: https://img.shields.io/github/issues/hyperledger/quilt.svg
[github-issues-url]: https://github.com/hyperledger/quilt/issues
