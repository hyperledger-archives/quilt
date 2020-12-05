package org.interledger.link.spsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.core.InterledgerConstants.ALL_ZEROS_FULFILLMENT;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.primitives.UnsignedLong;
import org.interledger.core.AmountTooLargeErrorData;
import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerConstants;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.link.LinkId;
import org.interledger.stream.receiver.StreamReceiver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link StatelessStreamReceiverLink}.
 */
public class StatelessStreamReceiverLinkTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.foo");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamReceiver streamReceiverMock;

  private StatelessStreamReceiverLink link;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.link = new StatelessStreamReceiverLink(
      () -> OPERATOR_ADDRESS,
      StatelessSpspReceiverLinkSettings.builder().assetCode("XRP").assetScale((short) 9).build(),
      streamReceiverMock
    );
    link.setLinkId(LinkId.of("foo"));
  }

  @Test(expected = RuntimeException.class)
  public void registerLinkHandler() {
    try {
      link.registerLinkHandler(incomingPreparePacket -> null);
    } catch (Exception e) {
      assertThat(e.getMessage())
        .isEqualTo("StatelessSpspReceiver links never emit data, and thus should not have a registered DataHandler.");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void sendPacketWithNull() {
    try {
      link.sendPacket(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("preparePacket must not be null");
      throw e;
    }
  }

  @Test
  public void sendPacket() {
    final InterledgerFulfillPacket actualFulfillPacket = InterledgerFulfillPacket.builder()
      .fulfillment(ALL_ZEROS_FULFILLMENT)
      .build();
    when(streamReceiverMock.receiveMoney(any(), any(), any())).thenReturn(actualFulfillPacket);

    final InterledgerPreparePacket preparePacket = preparePacket();
    link.sendPacket(preparePacket).handle(
      fulfillPacket -> {
        assertThat(fulfillPacket).isEqualTo(actualFulfillPacket);
      },
      rejectPacket -> {
        logger.error("rejectPacket={}", rejectPacket);
        fail("Expected a Fulfill");
      }
    );
  }

  /**
   * Send a packet that's too big, when maxPacketAmount is specified.
   */
  @Test
  public void sendPacketTooBig() {
    this.link = new StatelessStreamReceiverLink(
      () -> OPERATOR_ADDRESS,
      StatelessSpspReceiverLinkSettings.builder()
        .assetCode("XRP")
        .assetScale((short) 9)
        .maxPacketAmount(UnsignedLong.ONE) // <-- max packet is 1
        .build(),
      streamReceiverMock
    );
    link.setLinkId(LinkId.of("foo"));

    final InterledgerPreparePacket preparePacket = preparePacket();
    link.sendPacket(preparePacket).handle(
      fulfillPacket -> {
        fail("Packet with amount 10 should not fulfill because it's too large");
      },
      rejectPacket -> {
        logger.error("rejectPacket={}", rejectPacket);
        assertThat(rejectPacket.typedData()).isPresent();
        assertThat(rejectPacket.typedData().get()).isInstanceOf(AmountTooLargeErrorData.class);
        AmountTooLargeErrorData amountTooLargeErrorData = (AmountTooLargeErrorData) rejectPacket.typedData().get();
        assertThat(amountTooLargeErrorData.maximumAmount()).isEqualTo(UnsignedLong.ONE);
        assertThat(amountTooLargeErrorData.receivedAmount()).isEqualTo(UnsignedLong.valueOf(10L));
      }
    );

    verifyZeroInteractions(streamReceiverMock);
  }

  private InterledgerPreparePacket preparePacket() {
    return InterledgerPreparePacket.builder()
      .amount(UnsignedLong.valueOf(10L))
      .executionCondition(InterledgerConstants.ALL_ZEROS_CONDITION)
      .destination(OPERATOR_ADDRESS)
      .expiresAt(DateUtils.now())
      .data(new byte[32])
      .build();
  }
}
