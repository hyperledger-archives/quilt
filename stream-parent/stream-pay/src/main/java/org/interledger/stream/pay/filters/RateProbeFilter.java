package org.interledger.stream.pay.filters;

/**
 * Determines how the maximum packet amount is known or discovered.
 *
 * @deprecated replace with ExchangeRateProbeService.
 */
@Deprecated
public class RateProbeFilter { // implements StreamPacketFilter {

//  // Static because these filters will be constructed a lot.
//  private static final Logger LOGGER = LoggerFactory.getLogger(RateProbeFilter.class);
//
//  private static final Duration TIMEOUT = Duration.of(10, ChronoUnit.SECONDS);
//
//  private AtomicReference<Instant> deadline;
//
//  private final Set<UnsignedLong> inFlightAmounts;
//  private final Set<UnsignedLong> ackedAmounts;
//
//  private final List<Long> initialTestPacketAmounts;
//
//  private final PaymentSharedStateTracker paymentSharedStateTracker;
//
//  public RateProbeFilter(final PaymentSharedStateTracker paymentSharedStateTracker) {
//    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
//    this.deadline = new AtomicReference<>();
//    this.inFlightAmounts = Sets.newConcurrentHashSet();
//    this.ackedAmounts = Sets.newConcurrentHashSet();
//    this.initialTestPacketAmounts = Lists.newArrayList(
//        0L,
//        1_000_000_000_000L,
//        1_000_000_000_00L,
//        1_000_000_000_0L,
//        1_000_000_000L,
//        1_000_000_00L,
//        1_000_000_0L,
//        1_000_000L,
//        1_000_00L,
//        1_000_0L,
//        1_000L,
//        100L,
//        10L,
//        1L
//    );
//  }
//
//  @Override
//  public StreamPacketReply doFilter(
//      final ModifiableStreamPacketRequest streamRequest, final StreamPacketFilterChain filterChain
//  ) {
//    Objects.requireNonNull(streamRequest);
//    Objects.requireNonNull(filterChain);
//
//    this.inFlightAmounts.add(streamRequest.sourceAmount());
//
//    // Set deadline when the first test packet is sent
//    if (this.deadline.get() == null) {
//      this.deadline.compareAndSet(null, Instant.now().plus(TIMEOUT));
//    }
//
//    if (FluentCompareTo.is(Instant.now()).greaterThan(deadline.get())) {
//      return StreamPacketReply.builder()
//          .sendState(SendState.RateProbeFailed)
//          .build();
//    }
//
//    if (this.initialTestPacketAmounts.size() > 0) {
//      UnsignedLong nextTestPacket = UnsignedLong.valueOf(this.initialTestPacketAmounts.get(0));
//      streamRequest.setSourceAmount(nextTestPacket);
//      // TODO:
//      //sendPacket();
//      return StreamPacketReply.builder().sendState(SendState.Wait).build();
//    } else {
//      // No more initialTestPacketAmounts to send.
//      final Optional<UnsignedLong> knownMaxPacketAmount = null; //paymentSharedStateService.getDiscoveredMaxPacketAmount();
//
//      // If there are no more inFlightAmounts to wait for, then we can finish.
//      if (knownMaxPacketAmount.isPresent() && this.inFlightAmounts.size() == 0) {
//        // TODO: paymentSharedStateService.setKnownMaxPacketAmount;
//        return StreamPacketReply.builder().sendState(SendState.End).build();
//      }
//
//
//    }
//
//    ///////////////////
//    // Execute the chain
//    ///////////////////
//    StreamPacketReply streamReply = filterChain.doFilter(streamRequest);
//
//    // TODO:
//    this.inFlightAmounts.remove(streamRequest.sourceAmount());
//    if (streamReply.isAuthentic()) {
//      this.ackedAmounts.add(streamRequest.sourceAmount());
//    }
//
//    return streamReply;
//  }

}
