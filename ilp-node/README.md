## Interledger Node Framework

This module is a framework that can be used to quickly implement an ILP node.

_**IMPORTANT:** This is alpha quality and is still being incubated. It may change a lot but our intent is to be additive where possible as opposed to doing any major refactoring._

### Design Goals

  1. Simple, scalable architecture
  2. Un-opinionated on the use of frameworks for the implementation
  3. Pluggable service-based architecture allowing services to be easily replaced with custom implementations
  3. Minimal implementations of required services
  
### Architecture

  * An ILP node is configured with one or more _Accounts_. 
  * An account has a _Channel_ which must instantiated at startup and _opened_ before it can be used.
  * A _Channel_ exposes a similar interface to the LedgerPlugin Interface v2 defined in the JS implementation of Interledger (more below).
  * _Channel_ implementations are required for each unique account-relationship type. For example an account between two nodes that uses XRP Payment Channels will require a channel implementation that is able to open and close payment channels and exchange payment channel claims.
  * _Channel_ implementations are the equivalent of _plug-ins_ in the JS reference stack. 
  * An _Account_ has exactly one _Channel_ although it is possible for multiple _Accounts_ to all have unique instances of the same _Channel_ implementation.
  * The _Node_ instance should manage the state of all _Channels_ and handle errors raised by the _Channel_ by closing (and then possibly re-opening) the _Channel_.
  * The _Node_ responds to incoming ILP packets from a _Channel_ by routing them to another _Channel_.
  * When routing an ILP Prepare packet the node maintains state and routes the corresponding ILP Fulfill or ILP Reject packets back to the same _Account_ that was the source of the original ILP Prepare.
  * Routing, currency conversion, logging, internal dispatching, packet processing are all exposed as services that can be replaced with cust0m implementations.

### Scaling

The current architecture assumes that each service will have access to a thread pool. An incoming request from a channel is dispatched for processing on its own thread.

The creation and maintenance of the thread pool is left to the implementation.

For implementations that wish to have large numbers of accounts with a single external endpoint that aggregates the channels this shared endpoint should be accessible from each channel instance.

Alternatively, the multi-account processing could be left to the channel implementation which could internally implement a simple _Node_. 

### TODO

  * Finish implementation and testing of ILDCP (possibly factor out into it's own module)
  * Implement routing protocol (exchange of routes with peers and updates to routing table)
  * Tests for default services
