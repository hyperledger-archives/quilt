package org.interledger.stream.sender.good;

import org.interledger.link.Link;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.sender.StreamConnectionManager;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultStreamSenderTest {

  @Mock
  private Link linkMock;
  @Mock
  private StreamConnectionManager streamConnectionManagerMock;
  @Mock
  private StreamEncryptionUtils streamEncryptionUtilsMock;

  @Mock
  private PaymentTracker paymentTrackerMock;

  private DefaultStreamSender defaultStreamSender;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
//    defaultStreamSender = new DefaultStreamSender(
//        linkMock, streamConnectionManagerMock, streamEncryptionUtilsMock,  paymentTrackerMock
//    );
  }

}
