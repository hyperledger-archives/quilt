# Hyperledger Quilt [![Discuss][forum-image]][forum-url] [![twitter][twitter-image]][twitter-url]
[![circle-ci][circle-image]][circle-url] 
[![codecov][codecov-image]][codecov-url] 
[![lgtm-cq][lgtm-cq-image]][lgtm-cq-url] 
[![lgtm-alerts][lgtm-alerts-image]][lgtm-alerts-url]
[![issues][github-issues-image]][github-issues-url]

Quilt is a Java implementation of the [Interledger](https://interledger.org) protocol. 

This library can be used to send and receive Interledger payments using STREAM. It also supports a variety of other core Interledger primitives and protocols.

Note that this library does not implement a full Java Connector. For this functionality, see the [Java ILP Connector](https://github.com/sappenin/java-ilpv4-connector) project, which is built using Quilt. 

## Modules

Quilt is organised as a Maven multi-module project. Each module exists in a subdirectory that has its own child POM and README file. Dependency and plugin versions are managed in the parent project.

Issues are labelled and prefixed to make it easy to identify which project they relate to.

### ilp-core [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-core.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-core)
The `ilp-core` module is the base library for any Interledger Java projects, providing service interfaces, packet definitions, and data models supporting the ILPv4 protocol (defined in [IL-RFC-27](https://github.com/interledger/rfcs/blob/master/0027-interledger-protocol-4/0027-interledger-protocol-4.md)). 

These primitives form the foundation of the Interledger suite of protocols, and are used throughout the other modules in this project.

| ILP Module       | Javadoc Link                                                                                                                                            |
|-------------:|---------------------------------------------------------------------------------------------------------------------------------------------------------| 
| `ilp-core`   | [![Javadocs](https://www.javadoc.io/badge/org.interledger/ilp-core.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/ilp-core) | 

[READ MORE](./ilp-core)

### btp-core [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/btp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Abtp)
The `btp-core` module contains primitives to support the Bilateral Transfer Protocol (BTP v2.0) as defined by [IL-RFC-23](https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md).

| BTP Module       | Javadoc Link                                                                                                                                            |
|-------------:|---------------------------------------------------------------------------------------------------------------------------------------------------------| 
| `btp-core`   | [![Javadocs](https://www.javadoc.io/badge/org.interledger/btp-core.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/btp-core)  |  

### codecs-parent
The `codecs-parent` module contains an encoding and decoding framework plus serializers for ASN.1 OER formats defined in each IL-RFC. This module supports all primitives for the following protocols: [ILP v4.0](https://github.com/interledger/rfcs/blob/master/0027-interledger-protocol-4/0027-interledger-protocol-4.md), [IL-DCP v1.0](https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md), [BTP 2.0](https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md), and [STREAM v1.0](https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md).

| Codec Module       | Javadoc Link                                                                                                                                                                   |
|-------------------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `codecs-framework` | [![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-framework.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/codecs-framework) |
| `codecs-btp`       | [![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-btp.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/codecs-btp)                   |
| `codecs-ilp`       | [![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-ilp.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/codecs-ilp)                   |
| `codecs-ildcp`     | [![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-ildcp.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/codecs-ildcp)             |
| `codecs-stream`    | [![Javadocs](https://www.javadoc.io/badge/org.interledger/codecs-stream.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/codecs-stream)          |

### ildcp-core [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ildcp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Aildcp)
The `ildcp-core` module contains primitives to support the Interledger Dynamic Configuration Protocol (IL-DCP v1.0) as defined by [IL-RFC-31](https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md).

| IL-DCP Module          | Javadoc Link                                                                                                                                      |
|---------------------:|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `ildcp-core`          | [![Javadocs](https://www.javadoc.io/badge/org.interledger/ildcp-core.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/ildcp-core)                  |

### jackson-datatypes [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/jackson.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ajackson)
The `jackson-datatypes` module contains utilities to marshal and unmarshal various Quilt primitives to and from JSON using the [Jackson library](https://github.com/FasterXML/jackson).

| Jackson Module      | Javadoc Link |
|--------------------:|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `jackson-datatypes` | [![Javadocs](https://www.javadoc.io/badge/org.interledger/jackson-datatypes.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/jackson-datatypes) |

### link-parent
The `link-parent` module contains libraries that can be used as a network transport for Interledger accounts. Currently supported links are ILP-over-HTTP as defined in [IL-RFC-35](https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md).

| Link Module          | Javadoc Link                                                                                                                                      |
|---------------------:|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `link-core`          | [![Javadocs](https://www.javadoc.io/badge/org.interledger/link-core.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/link-core)                   |
| `link-ilp-over-http` | [![Javadocs](https://www.javadoc.io/badge/org.interledger/link-ilp-over-http.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/link-ilp-over-http) |

### spsp-parent [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/spsp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Aspsp)
The `spsp-parent` module contains libraries that can be used to resolve [Payment Pointers](https://paymentpointers.org/) (as defined in [IL-RFC-26](https://github.com/interledger/rfcs/blob/master/0026-payment-pointers/0026-payment-pointers.md) as well as the broader Simple Payment Setup Protocol (SPSP) as defined in [IL-RFC-009](https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md).

| SPSP Module   | Javadoc Link                                                                                                                        |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `spsp-core`   | [![Javadocs](https://www.javadoc.io/badge/org.interledger/spsp-core.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/spsp-core)     |
| `spsp-client` | [![Javadocs](https://www.javadoc.io/badge/org.interledger/spsp-client.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/spsp-client) |

### stream-parent [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/stream.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Astream)
The `stream-parent` module contains libraries for sending and receiving packetized payments using the STREAM protocol, defined in [IL-RFC-29](https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md).

| Stream Module     | Javadoc Link                                                                                                                                |
|------------------:|---------------------------------------------------------------------------------------------------------------------------------------------|
| `stream-core`     | [![Javadocs](https://www.javadoc.io/badge/org.interledger/stream-core.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/stream-core)         |
| `stream-client`   | [![Javadocs](https://www.javadoc.io/badge/org.interledger/stream-client.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/stream-client)     |
| `stream-receiver` | [![Javadocs](https://www.javadoc.io/badge/org.interledger/stream-receiver.svg?color=blue)](https://www.javadoc.io/doc/org.interledger/stream-receiver) |

### examples [![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/examples.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Aexamples)
The `examples` modules shows how to use the Quilt library in your code.

[READ MORE](./examples-parent)

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
[codacy-image]: https://api.codacy.com/project/badge/Grade/875a1b0b076a4a7399fa43d0c6f27748
[codacy-url]: https://www.codacy.com/manual/xpring/quilt?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=hyperledger/quilt&amp;utm_campaign=Badge_Grade
[lgtm-cq-image]: https://img.shields.io/lgtm/grade/java/g/hyperledger/quilt.svg?logo=lgtm&logoWidth=18
[lgtm-cq-url]: https://lgtm.com/projects/g/hyperledger/quilt/context:java
[lgtm-alerts-image]: https://img.shields.io/lgtm/alerts/g/hyperledger/quilt.svg?logo=lgtm&logoWidth=18
[lgtm-alerts-url]: https://lgtm.com/projects/g/hyperledger/quilt/alerts/
[twitter-image]: https://img.shields.io/twitter/follow/interledger.svg?style=social
[twitter-url]: https://twitter.com/intent/follow?screen_name=interledger
[github-issues-image]: https://img.shields.io/github/issues/hyperledger/quilt.svg
[github-issues-url]: https://github.com/hyperledger/quilt/issues
