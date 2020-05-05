package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.UUID;

/**
 * Unit tests for {@link SendMoneyRequest}.
 */
public class SendMoneyRequestTest {

  private static final UUID RANDOM_UUID = UUID.randomUUID();
  private static final Denomination DENOMINATION = Denomination.builder().assetCode("USD").assetScale((short) 2)
      .build();
  private static final SharedSecret SHARED_SECRET = SharedSecret.of(new byte[32]);

  @Mock
  private PaymentTracker paymentTrackerMock;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testBuilderWithoutOptionals() {
    SendMoneyRequest result = SendMoneyRequest.builder()
        .amount(UnsignedLong.valueOf(10))
        .destinationAddress(InterledgerAddress.of("example.destination"))
        .sharedSecret(SHARED_SECRET)
        .denomination(DENOMINATION)
        .requestId(RANDOM_UUID)
        .paymentTracker(paymentTrackerMock)
        .build();

    assertThat(result.sourceAddress()).isEmpty();
    assertThat(result.amount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(result.destinationAddress()).isEqualTo(InterledgerAddress.of("example.destination"));
    assertThat(result.sharedSecret()).isEqualTo(SHARED_SECRET);
    assertThat(result.denomination()).isEqualTo(DENOMINATION);
    assertThat(result.timeout()).isEmpty();
    assertThat(result.requestId()).isEqualTo(RANDOM_UUID);
    assertThat(result.paymentTracker()).isEqualTo(paymentTrackerMock);
  }

  @Test
  public void testBuilderWithOptionals() {
    SendMoneyRequest result = SendMoneyRequest.builder()
        .sourceAddress(InterledgerAddress.of("example.sender"))
        .amount(UnsignedLong.valueOf(10))
        .destinationAddress(InterledgerAddress.of("example.destination"))
        .sharedSecret(SHARED_SECRET)
        .denomination(DENOMINATION)
        .timeout(Duration.ZERO)
        .requestId(RANDOM_UUID)
        .paymentTracker(paymentTrackerMock)
        .build();

    assertThat(result.sourceAddress().get()).isEqualTo(InterledgerAddress.of("example.sender"));
    assertThat(result.amount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(result.destinationAddress()).isEqualTo(InterledgerAddress.of("example.destination"));
    assertThat(result.sharedSecret()).isEqualTo(SHARED_SECRET);
    assertThat(result.denomination()).isEqualTo(DENOMINATION);
    assertThat(result.timeout().get()).isEqualTo(Duration.ZERO);
    assertThat(result.requestId()).isEqualTo(RANDOM_UUID);
    assertThat(result.paymentTracker()).isEqualTo(paymentTrackerMock);
  }
}
