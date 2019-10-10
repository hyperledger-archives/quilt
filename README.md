# Hyperledger Quilt [![Discuss][forum-image]][forum-url] [![twitter][twitter-image]][twitter-url]
[![circle-ci][circle-image]][circle-url] [![codecov][codecov-image]][codecov-url] [![codacy][codacy-image]][codacy-url] [![issues][github-issues-image]][github-issues-url]

Quilt is an implementation of the [Interledger](https://interledger.org) protocol in Java.

## Modules

The quilt project is organised as a Maven multi-module project. Each module exists in a subdirectory and has its own
POM and README.

Dependency and plugin versions are managed in the parent project.

Issues are labelled and prefixed to make it easy to identify which project they relate to.

### ilp-core

The `ilp-core` module is the base library for any Interledger projects providing service interfaces, event descriptions,
exceptions and data models supporting ILPv4 as defined in [IL-RFC-27](https://github.com/interledger/rfcs/blob/master/0027-interledger-protocol-4/0027-interledger-protocol-4.md)

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-core.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-core)

[READ MORE](./ilp-core)

### btp-core
The `btp-core` module contains primitives to support the Bilateral Transfer Protocol (BTP v2.0) as defined by [IL-RFC-23](https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md).

[READ MORE](./btp-core)

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/btp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Abtp)

### codecs-parent
The `codecs-parent` module contains an encoding and decoding framework plus serializers for ASN.1 OER formats defined in each IL-RFC. This module supports all primitives for the following protocols: [ILP v4.0](https://github.com/interledger/rfcs/blob/master/0027-interledger-protocol-4/0027-interledger-protocol-4.md), [IL-DCP v1.0](https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md), [BTP 2.0](https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md), and [STREAM v1.0](https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md).

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/codecs.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Acodecs)

### ildcp-core
The `ildcp-core` module contains primitives to support the Interledger Dynamic Configuration Protocol (IL-DCP v1.0) as defined by [IL-RFC-31](https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md).

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ildcp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Aildcp)

[READ MORE](./ildcp-core)

### jackson-datatypes
The `jackson-datatypes` module contains utilities to marshal and unmarshal various Quilt primitives to and from JSON using the [Jackson library](https://github.com/FasterXML/jackson).

[READ MORE](./jackson-datatypes)

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/jackson.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ajackson)

### link-parent
The `link-parent` module contains libraries that can be used as a network transport for Interledger accounts. Currently supported links are ILP-over-HTTP as defined in [IL-RFC-35](https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md).

[READ MORE](./link-parent)

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-link.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-link)

### spsp-parent
The `spsp-parent` module contains libraries that can be used to resolve [Payment Pointers](https://paymentpointers.org/) (as defined in [IL-RFC-26](https://github.com/interledger/rfcs/blob/master/0026-payment-pointers/0026-payment-pointers.md) as well as the broader Simple Payment Setup Protocol (SPSP) as defined in [IL-RFC-009](https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md).
[READ MORE](./spsp-parent)

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/spsp.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Aspsp)

### stream-parent
The `stream-parent` module contains libraries for sending and receiving packetized payments using the STREAM protocol, defined in [IL-RFC-29](https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md).

[READ MORE](./stream-parent)

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/stream.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Astream)

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

#### Maven
This project uses Maven to manage dependencies and other aspects of the build. 
To install Maven, follow the instructions at [https://maven.apache.org/install.html](https://maven.apache.org/install.html).

Modules in this library can be included in your project by first adding a Snapshot Repository to your `pom.xml` file, like this:

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
    <artifactId>java-ilp-core</artifactId>
    <version>1.0-SNAPSHOT</version>
  </dependency>
  ...
</dependencies>
```

#### Gradle
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
    compile group: 'org.interledger', name: 'java-ilp-core', version: '1.0-SNAPSHOT'
    ...
}
```

### Docker
By default, the build runs integration tests which depend on Docker daemon to be running. If you don't have Docker running, you can
skip integration tests using `-DskipITs` (e.g. `mvn clean install -DskipITs`).

Otherwise, to install docker, follow the instructions at [https://docs.docker.com/install/](https://docs.docker.com/install/) 

## Development
We welcome any and all submissions, whether it's a typo, bug fix, or new feature. To get started, first download the code:

``` sh
git clone https://github.com/hyperledger/quilt
cd quilt
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

[forum-url]: https://forum.interledger.org/tags/java-ilpv4-connector
[forum-image]: https://img.shields.io/badge/Discuss-Interledger%20Forum-blue.svg
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
