package org.interledger.stream.receiver;

import com.google.common.primitives.UnsignedLong;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.StreamConnectionDetails;
import org.interledger.stream.StreamException;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StatelessStreamReceiver}.
 */
public class StatelessStreamReceiverTest {

  @Mock
  private CodecContext mockCodecContext;

  private ServerSecretSupplier serverSecretSupplier;

  private CodecContext streamCodecContext;

  private StreamConnectionGenerator streamConnectionGenerator;

  private StreamEncryptionService streamEncryptionService;

  private StreamReceiver streamReceiver;

  private StreamConnectionDetails connectionDetails;

  private StreamPacket testStreamPacket;

  private byte[] encryptedStreamPacketBytes;

  private InterledgerCondition executionCondition;

  private InterledgerPreparePacket preparePacket;

  private final InterledgerAddress clientAddress = InterledgerAddress.of("example.destination");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    this.streamConnectionGenerator = new SpspStreamConnectionGenerator();
    this.streamEncryptionService = new JavaxStreamEncryptionService();
    this.streamCodecContext = StreamCodecContextFactory.oer();

    this.serverSecretSupplier = () -> new byte[32];
    this.streamReceiver = new StatelessStreamReceiver(
        serverSecretSupplier, streamConnectionGenerator, streamEncryptionService,
        StreamCodecContextFactory.oer()
    );

    this.connectionDetails = streamConnectionGenerator
        .generateConnectionDetails(serverSecretSupplier, clientAddress);

    this.testStreamPacket = createStreamPacket(UnsignedLong.ZERO);

    encryptedStreamPacketBytes = createEncryptedStreamPacketBytes(testStreamPacket);

    executionCondition = StreamUtils
        .generatedFulfillableFulfillment(connectionDetails.sharedSecret().getBytes(), encryptedStreamPacketBytes).getCondition();

    preparePacket = createPreparePacket(executionCondition);
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullServerSecret() {
    try {
      new StatelessStreamReceiver(null, new SpspStreamConnectionGenerator(), streamEncryptionService,
          StreamCodecContextFactory.oer());
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("serverSecretSupplier must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullConnectionGenerator() {
    try {
      new StatelessStreamReceiver(serverSecretSupplier, null, streamEncryptionService,
          StreamCodecContextFactory.oer());
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("connectionGenerator must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullStreamEncryptionService() {
    try {
      new StatelessStreamReceiver(serverSecretSupplier, streamConnectionGenerator, null,
          StreamCodecContextFactory.oer());
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("streamEncryptionService must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullCodecContextFactory() {
    try {
      new StatelessStreamReceiver(
          serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, null
      );
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("streamCodecContext must not be null");
      throw e;
    }
  }

  @Test
  public void setupStream() {
    StreamConnectionGenerator streamConnectionGenerator = mock(StreamConnectionGenerator.class);
    this.streamReceiver = new StatelessStreamReceiver(
      serverSecretSupplier, streamConnectionGenerator, streamEncryptionService,
      StreamCodecContextFactory.oer()
    );

    InterledgerAddress receiverAddress = InterledgerAddress.of("example.receiver");
    streamReceiver.setupStream(receiverAddress);

    Mockito.verify(streamConnectionGenerator).generateConnectionDetails(Mockito.any(), eq(receiverAddress));
  }

  @Test
  public void testFulfillsValidPacket() throws IOException {
    streamReceiver = new StatelessStreamReceiver(
      serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, StreamCodecContextFactory.oer()
    );
    final InterledgerAddress receiverAddress = InterledgerAddress.of("example.receiver");

    final StreamConnectionDetails connectionDetails = streamConnectionGenerator
      .generateConnectionDetails(serverSecretSupplier, receiverAddress);

    final StreamPacket testStreamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .prepareAmount(UnsignedLong.ZERO)
      .sequence(UnsignedLong.ONE)
      .addFrames(StreamMoneyFrame.builder()
        .streamId(UnsignedLong.ONE)
        .shares(UnsignedLong.ONE)
        .build())
      .build();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    streamCodecContext.write(testStreamPacket, baos);
    final byte[] encryptedStreamPacketBytes = streamEncryptionService
      .encrypt(Base64.getDecoder().decode(connectionDetails.sharedSecret()), baos.toByteArray());

    final InterledgerCondition executionCondition = StreamUtils
      .generatedFulfillableFulfillment(Base64.getDecoder().decode(connectionDetails.sharedSecret()), encryptedStreamPacketBytes).getCondition();

    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .destination(connectionDetails.destinationAddress())
      .amount(BigInteger.valueOf(100L))
      .expiresAt(Instant.EPOCH)
      .data(encryptedStreamPacketBytes)
      .executionCondition(executionCondition)
      .build();

    this.streamReceiver.receiveMoney(preparePacket, receiverAddress)
      .handle((fulfillPacket ->
        assertThat(fulfillPacket.getFulfillment().getCondition()).isEqualTo(executionCondition)),
        rejectPacket -> fail("should have fulfilled"));
  }

  @Test
  public void receiveMoneyRejectsOnDecryption() throws Exception {
    this.streamReceiver = new StatelessStreamReceiver(
        serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, mockCodecContext
    );

    when(mockCodecContext.read(any(), any())).thenThrow(new IOException());

    InterledgerRejectPacket expected = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT)
        .message("Could not decrypt data")
        .triggeredBy(clientAddress)
        .build();

    this.streamReceiver.receiveMoney(preparePacket, clientAddress)
        .handle((fulfillPacket -> fail("should have rejected")),
          rejectPacket -> assertThat(rejectPacket).isEqualTo(expected));
  }

  @Test
  public void receiveMoneyThrowsOnWriteFailureWhenFulfillable() throws Exception {
    this.streamReceiver = new StatelessStreamReceiver(
        serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, mockCodecContext
    );

    when(mockCodecContext.read(any(), any())).thenAnswer((Answer<StreamPacket>) invocation ->
      streamCodecContext.read(invocation.getArgument(0), invocation.getArgument(1)));

    doThrow(new IOException()).when(mockCodecContext).write(any(), any());

    expectedException.expect(StreamException.class);
    this.streamReceiver.receiveMoney(preparePacket, clientAddress);
  }

  @Test
  public void receiveMoneyThrowsOnWriteFailureWhenUnfulfillable() throws Exception {
    this.streamReceiver = new StatelessStreamReceiver(
        serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, mockCodecContext
    );
    StreamPacket unfulfillableStreamPacket = createStreamPacket(UnsignedLong.ONE);

    byte[] unfulfillableEncryptedStreamPacketBytes = createEncryptedStreamPacketBytes(unfulfillableStreamPacket);

    InterledgerCondition unfulfillableExecutionCondition = StreamUtils
        .generatedFulfillableFulfillment(connectionDetails.sharedSecret().getBytes(), unfulfillableEncryptedStreamPacketBytes).getCondition();

    InterledgerPreparePacket unfulfillablePrepare = createPreparePacket(unfulfillableExecutionCondition);

    when(mockCodecContext.read(any(), any())).thenAnswer((Answer<StreamPacket>) invocation ->
      streamCodecContext.read(invocation.getArgument(0), invocation.getArgument(1)));

    doThrow(new IOException()).when(mockCodecContext).write(any(), any());
    expectedException.expect(StreamException.class);
    this.streamReceiver.receiveMoney(unfulfillablePrepare, clientAddress);
  }

  @Test
  public void receiveMoneyWithUnfulfillablePrepareRejects() throws Exception {
    streamReceiver = new StatelessStreamReceiver(
        serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, StreamCodecContextFactory.oer()
    );
    StreamPacket unfulfillableStreamPacket = createStreamPacket(UnsignedLong.ONE);

    byte[] unfulfillableEncryptedStreamPacketBytes = createEncryptedStreamPacketBytes(unfulfillableStreamPacket);

    InterledgerCondition unfulfillableExecutionCondition = StreamUtils
        .generatedFulfillableFulfillment(connectionDetails.sharedSecret().getBytes(), unfulfillableEncryptedStreamPacketBytes).getCondition();

    InterledgerPreparePacket unfulfillablePrepare = createPreparePacket(unfulfillableExecutionCondition);

    this.streamReceiver.receiveMoney(unfulfillablePrepare, clientAddress)
        .handle((fulfillPacket -> fail("should have rejected")),
          rejectPacket -> assertThat(rejectPacket).extracting("code", "message", "triggeredBy")
            .containsExactly(
                InterledgerErrorCode.F99_APPLICATION_ERROR,
                "STREAM packet not fulfillable (prepare amount < stream packet amount)",
                Optional.of(clientAddress)
            ));
  }

  private InterledgerPreparePacket createPreparePacket(InterledgerCondition executionCondition) {
    return InterledgerPreparePacket.builder()
        .destination(connectionDetails.destinationAddress())
        .amount(BigInteger.valueOf(100L))
        .expiresAt(Instant.EPOCH)
        .data(encryptedStreamPacketBytes)
        .executionCondition(executionCondition)
        .build();
  }

  private byte[] createEncryptedStreamPacketBytes(StreamPacket testStreamPacket) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    streamCodecContext.write(testStreamPacket, baos);
    return streamEncryptionService
        .encrypt(Base64.getDecoder().decode(connectionDetails.sharedSecret()), baos.toByteArray());
  }
  
  private StreamPacket createStreamPacket(UnsignedLong prepareAmount) {
    return StreamPacket.builder()
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        .prepareAmount(prepareAmount)
        .sequence(UnsignedLong.ONE)
        .addFrames(StreamMoneyFrame.builder()
            .streamId(UnsignedLong.ONE)
            .shares(UnsignedLong.ONE)
            .build())
        .build();
  }
}
