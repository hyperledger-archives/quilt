package org.interledger.stream.pay;

/**
 * Unit tests for {@link RunLoop}.
 */
public class RunLoopTest {
//
//  @Mock
//  private Link link;
//  private RunLoop runLoop;
//
//  @Before
//  public void setUp() {
//    MockitoAnnotations.initMocks(this);
//
//    // New for each payment/quote/etc.
//    PaymentSharedStateTracker paymentSharedStateTracker = null;
//    //new PaymentSharedStateTracker(streamConnection);
//    List<StreamPacketFilter> streamPacketFilterList = Arrays.asList(
//      // First so all other controllers log the sequence number
//      new SequenceFilter(paymentSharedStateTracker)
//      // Fail-fast on terminal rejects or timeouts
//      // TODO: FailureController
//      // Fail-fast on destination asset detail conflict
//      // TODO: AccountController
//      // Fail-fast if max packet amount is 0
//      //new MaxPacketAmountFilter(maxPacketAmountService)
//      // Limit how frequently packets are sent and early return
////        .set(PacingController, new PacingController())
////        .set(AmountController, new AmountController(controllers))
////        .set(ExchangeRateController, new ExchangeRateController())
////        .set(RateProbe, new RateProbe(controllers))
//      // Ensure each controller processes reply before resolving Promises
////        .set(PendingRequestTracker, new PendingRequestTracker())
//
//    );
//
//    this.runLoop = new RunLoop(link, streamPacketFilterList, streamEncryptionUtils);
//  }
//
////  @Test
////  public void testValidRunLoopExit() {
////
////    when(link.sendPacket(any())).thenReturn(constructResponsePacket());
////
////    QuoteRequest quoteRequest = QuoteRequest.builder()
////      .amountToSend(UnsignedLong.valueOf(10L))
////      .paymentPointer(PaymentPointer.of("$ripplex.money/test"))
////      .build();
////
////    Quote actualQuote = runLoop.start(quoteRequest);
////  }
//
//  //////////////////
//  // Private Helpers
//  //////////////////
//
//  private InterledgerResponsePacket constructResponsePacket() {
//    return InterledgerFulfillPacket.builder()
//      .fulfillment(InterledgerFulfillment.of(new byte[32]))
//      .build();
//  }


}