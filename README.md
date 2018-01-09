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

### crypto-conditions

Crypto-conditions is an implementation of the crypto-conditions specification available as an Internet Draft:
[draft-thomas-crypto-conditions-03](https://datatracker.ietf.org/doc/draft-thomas-crypto-conditions/).

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/crypto-conditions.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Acrypto-conditions)

[READ MORE](./crypto-conditions)

### ilp-core

The ilp-core module is the base library for any Interledger projects providing service interfaces, event descriptions,
exceptions and data models. It also includes an encoding framework and codecs for encoding and decoding ILP objects
using the Octet Encoding Rules (OER).

[![GitHub issues](https://img.shields.io/github/issues-raw/hyperledger/quilt/ilp-core.svg)](https://github.com/hyperledger/quilt/issues?q=is%3Aissue+is%3Aopen+label%3Ailp-core)

[READ MORE](./ilp-core)


### dev-ops

Dev-ops is a module for shared build and test tools

[READ MORE](./dev-ops)

### Add Unlimited Strength Encryption Policy Files 

### Symptoms/Error

While running maven clean install for the maven modules, the following error is thrown: 

[ERROR] PreimageSha256ConditionTest.testConstructionUsingMultipleThreads:45->AbstractCryptoConditionTest.runConcurrent:57->AbstractCryptoConditionTest.assertConcurrent:95 Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent

### Cause

The Base Java JVM and SDK installs from Oracle are limited in strength for the cryptographic functions that they can perform due to import control restrictions by the governments of a few countries. 

To deploy quilt, one needs to add the unlimited strength policy files. Detailed Resolution steps below. 

### Resolution -- Add Unlimited Strength Encryption Policy Files

1. Download Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy from http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html

2. Uncompress and extract the downloaded file. The download includes a Readme.txt and two .jar files with the same names as the existing policy files.

Locate the two existing policy files on your machine:

local_policy.jar

US_export_policy.jar

On UNIX/Linux, look in <java-home>/lib/security/

On Windows, look in C:/Program Files/Java/jre<version>/lib/security/

Replace the existing policy files with the unlimited strength policy files you extracted.

3. Re-run maven clean install 
