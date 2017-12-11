package org.interledger.codecs;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacket;
import org.interledger.InterledgerPacket.Handler;
import org.interledger.InterledgerPacket.VoidHandler;
import org.interledger.InterledgerPacketType;
import org.interledger.ilp.oer.InterledgerAddressOerCodec;
import org.interledger.ilqp.QuoteLiquidityRequest;
import org.interledger.ilqp.packets.QuoteLiquidityRequestPacketType;
import org.interledger.ilqp.packets.QuoteLiquidityResponsePacketType;

import org.hamcrest.MatcherAssert;
import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.framework.CodecException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link CodecContext}.
 */
public class CodecContextTest {

  /**
   * Convenience method to validate that the passed in {@link InterledgerPacket} is a {@link
   * QuoteLiquidityRequest} with the appropriate values.
   *
   * @param packet  The {@link InterledgerPacket} to validate
   * @param address The expected destination {@link InterledgerAddress} in String format.
   * @param minutes The expected number of minutes for the destinationHoldDuration of the request.
   */
  private static void validateQuoteLiquidityRequest(final InterledgerPacket packet,
                                                    final String address,
                                                    final int minutes) {

    MatcherAssert.assertThat("The read packet is an instance of QuoteLiquidityRequest",
        packet instanceof QuoteLiquidityRequest);

    QuoteLiquidityRequest decoded = (QuoteLiquidityRequest) packet;
    MatcherAssert.assertThat("The decoded packet has the same InterledgerAddress",
        decoded.getDestinationAccount().getValue().equals(address));
    MatcherAssert.assertThat("The decoded packet has the same Duration",
        decoded.getDestinationHoldDuration().equals(Duration.ofMinutes(minutes)));
  }

  /**
   * Test that the {@link CodecContext} properly registers class-based {@link Codec} instances and
   * associates them with the provided class type.
   */
  @Test
  public void testBasicRegistrationPositive() {
    CodecContext context = new CodecContext();
    context.register(InterledgerAddress.class, new InterledgerAddressOerCodec());
    MatcherAssert.assertThat("The context has an InterledgerAddressCodec",
        context.hasRegisteredCodec(InterledgerAddress.class));
  }

  /**
   * Test that the {@link CodecContext} does not have an un-registered {@link Codec}.
   */
  @Test
  public void testBasicRegistrationNegative() {
    CodecContext context = new CodecContext();
    MatcherAssert.assertThat("The context does not have an InterledgerAddressCodec",
        !context.hasRegisteredCodec(InterledgerAddress.class));
  }

  /**
   * Test the the {@link CodecContext} properly registers class-based {@link Codec} instances that
   * are sub-classes of {@link InterledgerPacketCodec} allowing the context to decode unknown bytes
   * off the wire based on the first typeId header.
   */
  @Test
  public void testRegistrationWithInterledgerPacketCodecAssociation() {

    InterledgerCodecContext context = InterledgerCodecContextFactory.oer();

    /**
     * Create an encoded {@link QuoteLiquidityRequest} in bytes format.
     */
    byte[] bytes = createEncodedTestInstance("g.foo", 3);


    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {

      /**
       *  Attempt to read an {@link InterledgerPacket} from the {@link InputStream}.
       */
      InterledgerPacket packet = context.read(inputStream);

      /* Validate that the packet is as expected. */
      validateQuoteLiquidityRequest(packet, "g.foo", 3);
    } catch (IOException e) {
      MatcherAssert.assertThat("An IOException was not thrown", false);
    }
  }

  /**
   * Test the the {@link CodecContext} properly throws a {@link CodecException} if the {@link Codec}
   * is missing.
   */
  @Test
  public void testThrowCodecException() {

    /**
     * Create an encoded {@link QuoteLiquidityRequest} in bytes format.
     */
    byte[] bytes = createEncodedTestInstance("g.foo", 3);

    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
      /**
       *  Attempt to read an {@link InterledgerPacket} from the inputStream with no {@link Codec}
       *  registered.
       */
      new InterledgerCodecContext().read(inputStream);
    } catch (CodecException e) {
      MatcherAssert.assertThat("An CodecException was thrown", true);
    } catch (IOException e) {
      MatcherAssert.assertThat("An IOException was not thrown", false);
    }
  }

  /**
   * Test that an {@link IOException} is thrown if the wrong {@link
   * InterledgerPacketType} is provided.
   */
  @Test
  public void testIoExceptionOnReadWithWrongType() {

    /**
     * Create an encoded {@link QuoteLiquidityRequest} in bytes format.
     */
    byte[] bytes = createEncodedTestInstance("g.foo", 3);

    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
      /**
       * Attempt to read a packet from the {@link java.io.InputStream} with while specifying the
       * wrong {@link InterledgerPacketType}
       */
      InterledgerCodecContextFactory.oer().read(
          new QuoteLiquidityResponsePacketType(), inputStream);
    } catch (CodecException e) {
      MatcherAssert.assertThat("An CodecException was not thrown", false);
    } catch (IOException e) {
      MatcherAssert.assertThat("An IOException was thrown", true);
    }
  }

  /**
   * Test the {@link CodecContext} properly decodes an {@link InterledgerPacket} when provided with
   * the correct {@link InterledgerPacketType}.
   */
  @Test
  public void testDecodingAnInterledgerPacketWithTheCorrectPacketType() {

    InterledgerCodecContext context = InterledgerCodecContextFactory.oer();

    /**
     * Create an encoded {@link QuoteLiquidityRequest} in bytes format.
     */
    byte[] bytes = createEncodedTestInstance("g.foo", 3);

    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {

      /**
       * We are obliged to swallow the type field as the read with {@link InterledgerPacketType}
       * assumes that the packet type byte has been read.
       */
      //noinspection ResultOfMethodCallIgnored
      inputStream.read(); // swallow type field

      /* Read the packet from the input stream */
      InterledgerPacket packet = context.read(
          new QuoteLiquidityRequestPacketType(), inputStream);

      /* Validate that the packet is as expected. */
      validateQuoteLiquidityRequest(packet, "g.foo", 3);
    } catch (CodecException e) {
      MatcherAssert.assertThat("A CodecException was not thrown", false);
    } catch (IOException e) {
      MatcherAssert.assertThat("An IOException was not thrown", false);
    }
  }

  /**
   * Test the {@link CodecContext} properly decodes an {@link InterledgerPacket} from a byte array
   * when provided with the class of the packet.
   */
  @Test
  public void testDecodingAnInterledgerPacketWithBytes() {

    CodecContext context = InterledgerCodecContextFactory.oer();

    /**
     * Create an encoded {@link QuoteLiquidityRequest} in bytes format.
     */
    byte[] bytes = createEncodedTestInstance("g.foo", 3);

    /* Attempt to read a packet from the bytes */
    InterledgerPacket packet = context.read(QuoteLiquidityRequest.class, bytes);
    /* Validate that the packet is as expected. */
    validateQuoteLiquidityRequest(packet, "g.foo", 3);

  }

  /**
   * Test that the {@link VoidHandler} is called with a validated {@link InterledgerPacket}.
   */
  @Test
  public void ensureTheVoidHandlerIsCalled() {

    InterledgerCodecContext context = InterledgerCodecContextFactory.oer();

    /**
     * Create an encoded {@link QuoteLiquidityRequest} in bytes format.
     */
    byte[] bytes = createEncodedTestInstance("g.foo", 3);

    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {

      final AtomicBoolean callbackMade = new AtomicBoolean(false);

      /* Read the packet from the input stream and expect a callback to the handler */
      context.readAndHandle(inputStream, new VoidHandler() {
        @Override
        public void execute(InterledgerPacket packet) {
          /* Validate that the packet is as expected. */
          validateQuoteLiquidityRequest(packet, "g.foo", 3);
          /* Notify the AtomicBoolean that the callback was made to the Handler. */
          callbackMade.set(true);
        }
      });

      MatcherAssert.assertThat("The callback to the VoidHandler was made", callbackMade.get());

    } catch (CodecException e) {
      MatcherAssert.assertThat("n CodecException was not thrown", false);
    } catch (IOException e) {
      MatcherAssert.assertThat("An IOException was not thrown", false);
    }

  }

  /**
   * Test that the {@link Handler} is called with a validated {@link InterledgerPacket}.
   */
  @Test
  public void ensureTheHandlerIsCalled() {

    InterledgerCodecContext context = InterledgerCodecContextFactory.oer();

    /**
     * Create an encoded {@link QuoteLiquidityRequest} in bytes format.
     */
    byte[] bytes = createEncodedTestInstance("g.foo", 3);

    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {

      final AtomicBoolean callbackMade = new AtomicBoolean(false);

      context.readAndHandle(inputStream, new Handler<String>() {

        @Override
        public String execute(InterledgerPacket packet) {
          /* Validate that the packet is as expected. */
          validateQuoteLiquidityRequest(packet, "g.foo", 3);
          /* Notify the AtomicBoolean that the callback was made to the Handler. */
          callbackMade.set(true);
          return "true";
        }

      });

      MatcherAssert.assertThat("The callback to the Handler was made", callbackMade.get());

    } catch (CodecException e) {
      MatcherAssert.assertThat("A CodecException was not thrown", false);
    } catch (IOException e) {
      MatcherAssert.assertThat("An IOException was not thrown", false);
    }

  }

  /**
   * Test that the {@link CodecContext} encoding with a supplied class works by encoding and then
   * decoding an {@link QuoteLiquidityRequest} validating the values of the decoded against those
   * expected.
   */
  @Test
  public void testWriteWithSuppliedClass() {

    CodecContext context = InterledgerCodecContextFactory.oer();

    /* Make a round trip encode -> decode */
    byte[] bytes =
        context.write(QuoteLiquidityRequest.class, createTestInstance("g.foo", 3));
    QuoteLiquidityRequest decoded = context.read(QuoteLiquidityRequest.class, bytes);

    /* Validate the decoded request */
    MatcherAssert.assertThat("The decoded packet has the same InterledgerAddress",
        decoded.getDestinationAccount().getValue().equals("g.foo"));
    MatcherAssert.assertThat("The decoded packet has the same Duration",
        decoded.getDestinationHoldDuration().equals(Duration.ofMinutes(3)));
  }

  /**
   * Test that the {@link CodecContext} encoding without a supplied class works by encoding and then
   * decoding an {@link QuoteLiquidityRequest} validating the values of the decoded against those
   * expected.
   */
  @Test
  public void testWriteWithoutSuppliedClass() {

    CodecContext context = InterledgerCodecContextFactory.oer();

    /* Make a round trip encode -> decode */
    byte[] bytes =
        context.write(createTestInstance("g.foo", 3));
    QuoteLiquidityRequest decoded = context.read(QuoteLiquidityRequest.class, bytes);

    /* Validate the decoded request */
    MatcherAssert.assertThat("The decoded packet has the same InterledgerAddress",
        decoded.getDestinationAccount().getValue().equals("g.foo"));
    MatcherAssert.assertThat("The decoded packet has the same Duration",
        decoded.getDestinationHoldDuration().equals(Duration.ofMinutes(3)));
  }

  /**
   * Test that {@link CodecContext} encoding without a supplied class works by encoding and then
   * decoding an {@link QuoteLiquidityRequest}, and validating the values of the decoded against
   * those the expected values.
   */
  @Test
  public void testWriteToOutputStream() {

    CodecContext context = InterledgerCodecContextFactory.oer();

    /* Make a round trip encode -> decode */
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      /* Make a round trip encode -> decode */
      context.write(createTestInstance("g.foo", 3), baos);
      QuoteLiquidityRequest decoded = context.read(QuoteLiquidityRequest.class, baos.toByteArray());

      /* Validate the decoded request */
      MatcherAssert.assertThat("The decoded packet has the same InterledgerAddress",
          decoded.getDestinationAccount().getValue().equals("g.foo"));
      MatcherAssert.assertThat("The decoded packet has the same Duration",
          decoded.getDestinationHoldDuration().equals(Duration.ofMinutes(3)));

    } catch (IOException e) {
      MatcherAssert.assertThat("An IOException was not thrown", false);
    }

  }

  /**
   * Convenience method to generate an encoded instance of {@link QuoteLiquidityRequest} for use
   * in test cases.
   *
   * @param address The destination {@link InterledgerAddress} in String format.
   * @param minutes The number of minutes for the destinationHoldDuration of the request.
   * @return A byte array containing the bytes of the encoded {@link QuoteLiquidityRequest}
   */
  private byte[] createEncodedTestInstance(final String address, final int minutes) {

    CodecContext context = InterledgerCodecContextFactory.oer();

    /* Create a liquidity request */
    QuoteLiquidityRequest quoteLiquidityRequest = createTestInstance(address, minutes);

    /* Convert the request to bytes */
    return context.write(quoteLiquidityRequest);
  }

  /**
   * Convenience method to generate an instance of {@link QuoteLiquidityRequest} for use in test
   * cases.
   *
   * @param address The destination {@link InterledgerAddress} in String format.
   * @param minutes The number of minutes for the destinationHoldDuration of the request.
   * @return A {@link QuoteLiquidityRequest} instance
   */
  private QuoteLiquidityRequest createTestInstance(final String address, final int minutes) {
    // Create a liquidity request
    return new QuoteLiquidityRequest() {
      @Override
      public InterledgerAddress getDestinationAccount() {
        return new InterledgerAddress() {
          @Override
          public String getValue() {
            return address;
          }
        };
      }

      @Override
      public Duration getDestinationHoldDuration() {
        return Duration.ofMinutes(minutes);
      }
    };
  }

}
