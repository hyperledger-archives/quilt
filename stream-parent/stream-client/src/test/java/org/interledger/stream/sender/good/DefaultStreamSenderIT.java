package org.interledger.stream.sender.good;

import org.interledger.link.Link;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.fx.ExchangeRateService;
import org.interledger.stream.good.SendMoneyRequest;
import org.interledger.stream.sender.StreamConnectionManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Executors;

// TODO: Remove and replace with SendMoneyIT (see SimpleStreamSenderIT) because we don't want to checkin Xpring creds.
@Deprecated
public class DefaultStreamSenderIT {

  @Mock
  private Link linkMock;
  @Mock
  private StreamConnectionManager streamConnectionManagerMock;
  @Mock
  private StreamEncryptionUtils streamEncryptionUtilsMock;
  @Mock
  private ExchangeRateService exchangeRateServiceMock;
  @Mock
  private PaymentTracker paymentTrackerMock; // TODO:


  private DefaultStreamSender defaultStreamSender;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    defaultStreamSender = new DefaultStreamSender(
        streamConnectionManagerMock, streamEncryptionUtilsMock, exchangeRateServiceMock, linkMock,
        Executors.newSingleThreadExecutor()
    );
  }

  // TODO: Remove and replace with SendMoneyIT (see SimpleStreamSenderIT).
  @Test

  public void testSendMoneyViaXpring() {
    defaultStreamSender.sendMoney(
        SendMoneyRequest.builder()

            .build()
    );

  }

}
