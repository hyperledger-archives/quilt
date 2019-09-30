# Hyperledger Quilt [![Discuss][forum-image]][forum-url] [![twitter][twitter-image]][twitter-url]
[![circle-ci][circle-image]][circle-url] [![codecov][codecov-image]][codecov-url] [![codacy][codacy-image]][codacy-url] [![issues][github-issues-image]][github-issues-url]

Quilt is an implementation of the [Interledger](https://interledger.org) protocol in Java.

## Modules

The quilt project is organised as a Maven multi-module project. Each module exists in a subdirectory and has its own
POM and README.

Dependency and plugin versions are managed in the parent project.

Issues are labelled and prefixed to make it easy to identify which project they relate to.

### ilp-core

The ilp-core module is the base library for any Interledger projects providing service interfaces, event descriptions,
exceptions and data models. It also includes an encoding framework and codecs for encoding and decoding ILP objects
using the Octet Encoding Rules (OER).

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-core.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-core)

[READ MORE](./ilp-core)

### examples

The examples modules shows how to use the Quilt library in your code.

[READ MORE](./examples/README.md)

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
    <version>0.19-SNAPSHOT</version>
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
    compile group: 'org.interledger', name: 'java-ilp-core', version: '0.19-SNAPSHOT'
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
