# Hyperledger Quilt [![join the chat][rocketchat-image]][rocketchat-url] [![twitter][twitter-image]][twitter-url]
[![circle-ci][circle-image]][circle-url] [![codecov][codecov-image]][codecov-url] [![issues][github-issues-image]][github-issues-url]

[rocketchat-url]: https://chat.hyperledger.org/channel/quilt
[rocketchat-image]: https://open.rocket.chat/images/join-chat.svg
[circle-image]: https://circleci.com/gh/hyperledger/quilt.svg?style=shield
[circle-url]: https://circleci.com/gh/hyperledger/quilt
[codecov-image]: https://codecov.io/gh/hyperledger/quilt/branch/master/graph/badge.svg
[codecov-url]: https://codecov.io/gh/hyperledger/quilt
[twitter-image]: https://img.shields.io/twitter/follow/interledger.svg?style=social
[twitter-url]: https://twitter.com/intent/follow?screen_name=interledger
[github-issues-image]: https://img.shields.io/github/issues/hyperledger/quilt.svg
[github-issues-url]: https://github.com/hyperledger/quilt/issues

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


### dev-ops

Dev-ops is a module for shared build and test tools

[READ MORE](./dev-ops)

## Usage

### Requirements

#### Maven
This project uses Maven to manage dependencies and other aspects of the build. 
To install Maven, follow the instructions at [https://maven.apache.org/install.html](https://maven.apache.org/install.html).

#### Unlimited Strength Encryption Policy Files 
In order to properly build this project, you must download and install Java Cryptography Extension 
(JCE) Unlimited Strength Jurisdiction Policy files. For more details, follow the instructions 
[here](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html). 

### Get the code

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
