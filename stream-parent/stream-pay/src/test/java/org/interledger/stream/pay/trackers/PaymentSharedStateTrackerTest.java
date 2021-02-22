package org.interledger.stream.pay.trackers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.fx.OracleExchangeRateService;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.model.AccountDetails;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link PaymentSharedStateTracker}.
 */
public class PaymentSharedStateTrackerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamConnection streamConnectionMock;

  @Mock
  private OracleExchangeRateService oracleExchangeRateServiceMock;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(streamConnectionMock.getSourceAccountDetails()).thenReturn(mock(AccountDetails.class));
    when(streamConnectionMock.getDestinationAddress()).thenReturn(mock(InterledgerAddress.class));
  }

  @Test
  public void testNullConstructorWithNullStreamConnection() {
    expectedException.expect(NullPointerException.class);
    new PaymentSharedStateTracker(null, oracleExchangeRateServiceMock);
  }

  @Test
  public void testNullConstructorWithNullExchangeRateService() {
    expectedException.expect(NullPointerException.class);
    new PaymentSharedStateTracker(streamConnectionMock, null);
  }

  @Test
  public void testConstructor() {
    final PaymentSharedStateTracker tracker = new PaymentSharedStateTracker(
      streamConnectionMock, oracleExchangeRateServiceMock
    );
    assertThat(tracker.getStreamConnection()).isEqualTo(streamConnectionMock);
    assertThat(tracker.getExchangeRateTracker()).isNotNull();
    assertThat(tracker.getAssetDetailsTracker()).isNotNull();
    assertThat(tracker.getMaxPacketAmountTracker()).isNotNull();
    assertThat(tracker.getAmountTracker()).isNotNull();
    assertThat(tracker.getPacingTracker()).isNotNull();
  }
}