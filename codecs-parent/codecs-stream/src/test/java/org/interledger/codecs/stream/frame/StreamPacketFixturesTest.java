package org.interledger.codecs.stream.frame;

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

import org.interledger.codecs.stream.StreamCodecContextFactory;
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
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>This class tests the Java implementation of STREAM based on a set of pre-computed and validated test
 * vectors found in the file `ValidStreamFrameVectorsTest.json`.</p>
 */
@RunWith(Parameterized.class)
public class StreamPacketFixturesTest {

  private StreamTestFixture streamTestFixture;

  public StreamPacketFixturesTest(StreamTestFixture streamTestFixture) {
    this.streamTestFixture = Objects.requireNonNull(streamTestFixture);
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

    Path path = Paths
        .get(StreamPacketFixturesTest.class.getClassLoader().getResource("StreamPacketFixtures.json").toURI());
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

    // Base64-encoded Bytes...
    String wantBuffer = fixture.buffer();

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

    assertThat(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())).isEqualTo(wantBuffer);
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
