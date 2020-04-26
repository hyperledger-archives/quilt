package org.interledger.codecs.stream;

/*-
 * ========================LICENSE_START=================================
 * Crypto Conditions
 * %%
 * Copyright (C) 2016 - 2018 Ripple Labs
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.interledger.codecs.stream.frame.helpers.FixtureManager.checkInternetConnectivity;
import static org.interledger.codecs.stream.frame.helpers.FixtureManager.checksum;
import static org.interledger.codecs.stream.frame.helpers.FixtureManager.downloadFixtureFile;
import static org.interledger.codecs.stream.frame.helpers.FixtureManager.readProperties;

import org.interledger.codecs.stream.frame.helpers.StreamFrameFixture;
import org.interledger.codecs.stream.frame.helpers.StreamTestFixture;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPacketType;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionDataBlockedFrame;
import org.interledger.stream.frames.ConnectionDataMaxFrame;
import org.interledger.stream.frames.ConnectionMaxStreamIdFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ConnectionStreamIdBlockedFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamDataBlockedFrame;
import org.interledger.stream.frames.StreamDataFrame;
import org.interledger.stream.frames.StreamDataMaxFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyBlockedFrame;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.frames.StreamMoneyMaxFrame;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>This class tests the Java implementation of STREAM based on a set of pre-computed and validated test
 * vectors found in the file `ValidStreamFrameVectorsTest.json`.</p>
 */
@RunWith(Parameterized.class)
public class StreamPacketFixturesTest {

  /**
   * The {@link ClassRule} provides a {@code before()} method which is executed before the tests in the class are
   * executed. As a part of the class rule, we expect the validation of the checksum to pass on both the local test
   * fixtures file and the remote test fixtures file in the RFCs repository. Any change to either the local or the
   * remote file will result in a failure of this test.
   */
  @ClassRule
  public static ExternalResource externalResource = new ExternalResource() {
    @Override
    protected void before() throws Throwable {
      boolean checkNetworkStatus = checkInternetConnectivity();
      if (checkNetworkStatus) {
        boolean rfcStatus = checkFixtureRFCStalenessState();
        boolean localFixtureStatus = checkLocalFixtureFileState();
        if (!rfcStatus || !localFixtureStatus) {
          if (!localFixtureStatus) {
         //   throw new Exception("Local test Fixture does not match the expected file integrity and has changed");
          }
         // throw new Exception("Change in Checksum. Fixture file on RFC does not match the expected value");
        }
      } else {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.warn("No active Internet connection is available. Running test using only the local fixtures.");
      }
    }
  };
  private StreamTestFixture streamTestFixture;

  public StreamPacketFixturesTest(StreamTestFixture streamTestFixture) {
    this.streamTestFixture = Objects.requireNonNull(streamTestFixture);
  }

  /**
   * When the test is run, this method reads the data of the latest fixtures on the RFC repository located at {@code
   * stream.packetFixtures.file} in the {@code fixtures.properties} configuration file. The method computes the SHA256
   * checksum of the fixtures file located in the repository and checks the integrity of the file.
   *
   * <p>Any changes in the actual fixtures file should fail when comparing with {@code stream.packetFixtures.checksum}
   * property of the configuration and returns {@code false} in case of changes to the actual file.</p>
   *
   * <p>The checksum of the file can be computed using the {@code shasum} binary by running
   * {@code shasum -a 256 filename}.</p>
   *
   * @return {@code false} if the Fixtures in the RFC have changed, else {@code true}.
   */
  @SuppressWarnings( {"checkstyle:AbbreviationAsWordInName" })
  private static boolean checkFixtureRFCStalenessState() {
    Properties properties = readProperties();
    String fixtureUrl = (String) properties.get("stream.packetFixtures.file");
    String expectedCheckSum = (String) properties.get("stream.packetFixtures.checksum");
    String fileName = (String) properties.get("stream.packetFixtures.fileName");

    byte[] downloadedFixtureData;
    downloadedFixtureData = downloadFixtureFile(fixtureUrl, fileName);

    Objects.requireNonNull(downloadedFixtureData);
    if (downloadedFixtureData.length > 0) {
      String computedCheckSum = BaseEncoding.base16().encode(checksum(downloadedFixtureData)).toLowerCase();

      // File matches the expectation therefore the fixtures haven't changed in the RFCs. We can proceed with
      // running the test of the fixtures.
      return computedCheckSum.equals(expectedCheckSum);
    }
    return false;
  }

  /**
   * When the test is run, this method reads the local fixtures resource and performs a checksum on the file contents to
   * ensure the integrity of the file and compares it with the expected {@code stream.packetFixtures.checksum} value in
   * the properties configuration located at {@code fixtures.properties}.
   *
   * <p>This test is present to ensure that in case the file is updated locally, we also update the corresponding
   * checksum of the file.</p>
   *
   * @return {@code false} if the fixtures have changed locally, else {@code true}.
   */
  private static boolean checkLocalFixtureFileState() {
    Properties properties = readProperties();
    String expectedCheckSum = (String) properties.get("stream.packetFixtures.checksum");
    String fileName = (String) properties.get("stream.packetFixtures.fileName");

    try {
      Path path = Paths.get(StreamPacketFixturesTest.class.getClassLoader().getResource(fileName).toURI());
      byte[] buffer = Files.readAllBytes(path);
      Objects.requireNonNull(buffer);
      String computedCheckSum = BaseEncoding.base16().encode(checksum(buffer)).toLowerCase();
      return computedCheckSum.equals(expectedCheckSum);
    } catch (URISyntaxException | IOException e) {
      e.printStackTrace();
    }

    return false;
  }

  /**
   * Loads a list of tests based on the json-encoded test vector files.
   */
  @Parameters(name = "Test Vector {index}: {0}")
  public static Collection<StreamTestFixture> testVectorData() throws Exception {

    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    Properties properties = readProperties();
    String fileName = (String) properties.get("stream.packetFixtures.fileName");

    Path path = Paths.get(StreamPacketFixturesTest.class.getClassLoader().getResource(fileName).toURI());

    Stream<String> lines = Files.lines(path);
    String data = lines.collect(Collectors.joining("\n"));
    lines.close();

    List<StreamTestFixture> vectors = objectMapper.readValue(data, new TypeReference<List<StreamTestFixture>>() {
    });

    return vectors;
  }

  /**
   * This test parses the conditionBinary, serializes it as a URI, and validates that the generated URI matches
   * "conditionUri" from the test vector.
   */
  @Test
  public void testParseConditionBinary() throws IOException {

    StreamTestFixture fixture = this.streamTestFixture;

    StreamPacket wantPacket = StreamPacket.builder()
        .sequence(UnsignedLong.valueOf(fixture.packet().sequence()))
        .interledgerPacketType(InterledgerPacketType.fromCode(fixture.packet().packetType()))
        .prepareAmount(UnsignedLong.valueOf(fixture.packet().amount()))
        .frames(
            fixture.packet().frames().stream()
                .map(this::fromJson)
                .collect(Collectors.toList())
        )
        .build();

    // Deserialize...
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(fixture.buffer()));
    StreamPacket packet = StreamCodecContextFactory.oer().read(StreamPacket.class, byteArrayInputStream);

    assertThat(packet).isEqualTo(wantPacket);

    if (fixture.decodeOnly().isPresent() && fixture.decodeOnly().get()) {
      return;
    }

    // Serialize...
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    StreamCodecContextFactory.oer().write(wantPacket, byteArrayOutputStream);

    // Base64-encoded Bytes...
    String wantBuffer = fixture.buffer();

    assertThat(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())).isEqualTo(wantBuffer);
  }

  @Test
  public void ignoreInvalidFrames() throws IOException {
    byte[] packetWithInvalidFrameBytes = Base64.getDecoder().decode("AQwBAQEBAQICBgVnLmZvbwIBAA==");
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(packetWithInvalidFrameBytes);
    StreamPacket packet = StreamCodecContextFactory.oer().read(StreamPacket.class, byteArrayInputStream);

    assertThat(packet.frames().size()).isEqualTo(1);
    ConnectionNewAddressFrame validFrame = (ConnectionNewAddressFrame) packet.frames().get(0);
    assertThat(validFrame.sourceAddress()).isEqualTo(InterledgerAddress.of("g.foo"));
  }

  private StreamFrame fromJson(final StreamFrameFixture fixture) {

    StreamFrameType streamFrameType = StreamFrameType.fromCode(fixture.type());

    switch (streamFrameType) {
      case ConnectionClose: {
        return ConnectionCloseFrame.builder()
            .errorCode(ErrorCodes.of(fixture.errorCode().get()))
            .errorMessage(fixture.errorMessage())
            .build();
      }
      case ConnectionNewAddress: {
        return ConnectionNewAddressFrame.builder()
            .sourceAddress(InterledgerAddress.of(fixture.sourceAccount().get()))
            .build();
      }
      case ConnectionDataMax: {
        return ConnectionDataMaxFrame.builder()
            .maxOffset(UnsignedLong.valueOf(fixture.maxOffset().get()))
            .build();
      }
      case ConnectionDataBlocked: {
        return ConnectionDataBlockedFrame.builder()
            .maxOffset(UnsignedLong.valueOf(fixture.maxOffset().get()))
            .build();
      }
      case ConnectionMaxStreamId: {
        return ConnectionMaxStreamIdFrame.builder()
            .maxStreamId(UnsignedLong.valueOf(fixture.maxStreamId().get()))
            .build();
      }
      case ConnectionStreamIdBlocked: {
        return ConnectionStreamIdBlockedFrame.builder()
            .maxStreamId(UnsignedLong.valueOf(fixture.maxStreamId().get()))
            .build();
      }
      case StreamClose: {
        return StreamCloseFrame.builder()
            .streamId(UnsignedLong.valueOf(fixture.streamId().get()))
            .errorCode(ErrorCodes.of(fixture.errorCode().get()))
            .errorMessage(fixture.errorMessage().get())
            .build();
      }
      case StreamMoney: {
        return StreamMoneyFrame.builder()
            .streamId(UnsignedLong.valueOf(fixture.streamId().get()))
            .shares(UnsignedLong.valueOf(fixture.shares().get()))
            .build();
      }
      case StreamMoneyMax: {
        return StreamMoneyMaxFrame.builder()
            .streamId(UnsignedLong.valueOf(fixture.streamId().get()))
            .totalReceived(UnsignedLong.valueOf(fixture.totalReceived().get()))
            .receiveMax(UnsignedLong.valueOf(fixture.receiveMax().get()))
            .build();
      }
      case StreamMoneyBlocked: {
        return StreamMoneyBlockedFrame.builder()
            .streamId(UnsignedLong.valueOf(fixture.streamId().get()))
            .sendMax(UnsignedLong.valueOf(fixture.sendMax().get()))
            .totalSent(UnsignedLong.valueOf(fixture.totalSent().get()))
            .build();
      }
      case StreamData: {
        return StreamDataFrame.builder()
            .streamId(UnsignedLong.valueOf(fixture.streamId().get()))
            .offset(UnsignedLong.valueOf(fixture.offset().get()))
            .data(Base64.getDecoder().decode(fixture.data().get()))
            .build();
      }
      case StreamDataMax: {
        return StreamDataMaxFrame.builder()
            .streamId(UnsignedLong.valueOf(fixture.streamId().get()))
            .maxOffset(UnsignedLong.valueOf(fixture.maxOffset().get()))
            .build();
      }
      case StreamDataBlocked: {
        return StreamDataBlockedFrame.builder()
            .streamId(UnsignedLong.valueOf(fixture.streamId().get()))
            .maxOffset(UnsignedLong.valueOf(fixture.maxOffset().get()))
            .build();
      }
      case ConnectionAssetDetails: {
        return ConnectionAssetDetailsFrame.builder().sourceDenomination(
            Denomination.builder()
                .assetCode(fixture.sourceAssetCode().get())
                .assetScale(Short.valueOf(fixture.sourceAssetScale().get()))
                .build())
            .build();
      }
      default: {
        throw new RuntimeException("Unsupported Frame:" + streamFrameType);
      }
    }
  }
}
