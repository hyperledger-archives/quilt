PacketSender: Sends a single packet and waits for a response or a timeout. Updates shared state as it goes.

StreamSenderAggregator: Sends one or more packets; handles congestion; etc. 

StreamSender: Sends a full amount, waiting for a response. Has a timeout of its own.


=========

Parallelism: should we send more than one packet in parallel? Sure, fine because each packet in-flight will reduce the total to be sent (before allowing other packets to be scheduled) and in-flight packets that reject will add their value back into the pool to be sent.

