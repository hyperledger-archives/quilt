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

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.codecs.stream.frame.helpers.StreamFrameTestVector;
import org.interledger.core.InterledgerAddress;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionDataBlockedFrame;
import org.interledger.stream.frames.ConnectionDataMaxFrame;
import org.interledger.stream.frames.ConnectionMaxStreamIdFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ConnectionStreamIdBlockedFrame;
import org.interledger.stream.frames.ErrorCode;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>This class tests the Java implementation of STREAM based on a set of pre-computed and validated test
 * vectors found in the file `ValidStreamFrameVectorsTest.json`.</p>
 */
@RunWith(Parameterized.class)
public class ValidStreamFrameVectorsTest {

  private StreamFrameTestVector streamFrameTestVector;

  public ValidStreamFrameVectorsTest(StreamFrameTestVector streamFrameTestVector) {
    this.streamFrameTestVector = streamFrameTestVector;
  }

  /**
   * Loads a list of tests based on the json-encoded test vector files.
   */
  @Parameters(name = "Test Vector {index}: {0}")
  public static Collection<StreamFrameTestVector> testVectorData() throws Exception {
    Path path = Paths.get(ValidStreamFrameVectorsTest.class.getClassLoader()
        .getResource("ValidStreamFrameVectorsTest.json").toURI());

    Stream<String> lines = Files.lines(path);
    String data = lines.collect(Collectors.joining("\n"));
    lines.close();

    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    List<StreamFrameTestVector> vectors = objectMapper
        .readValue(data, new TypeReference<List<StreamFrameTestVector>>() {
        });

    return vectors;
  }

  /**
   * This test parses the conditionBinary, serializes it as a URI, and validates that the generated URI matches
   * "conditionUri" from the test vector.
   */
  @Test
  public void testParseConditionBinary() throws IOException {

    StreamFrameType type = StreamFrameType.valueOf(streamFrameTestVector.type());

    StreamFrame frameFromTestVectorFile = null;
    switch (type) {
      case ConnectionClose: {
        frameFromTestVectorFile = ConnectionCloseFrame.builder()
            .errorCode(ErrorCode.of(streamFrameTestVector.errorCode().get()))
            .errorMessage(streamFrameTestVector.errorMessage())
            .build();
        break;
      }
      case ConnectionNewAddress: {
        frameFromTestVectorFile = ConnectionNewAddressFrame.builder()
            .sourceAddress(InterledgerAddress.of(streamFrameTestVector.sourceAddress().get()))
            .build();
        break;
      }
      case ConnectionDataMax: {
        frameFromTestVectorFile = ConnectionDataMaxFrame.builder()
            .maxOffset(UnsignedLong.valueOf(streamFrameTestVector.maxOffset().get()))
            .build();
        break;
      }
      case ConnectionDataBlocked: {
        frameFromTestVectorFile = ConnectionDataBlockedFrame.builder()
            .maxOffset(UnsignedLong.valueOf(streamFrameTestVector.maxOffset().get()))
            .build();
        break;
      }
      case ConnectionMaxStreamId: {
        frameFromTestVectorFile = ConnectionMaxStreamIdFrame.builder()
            .maxStreamId(UnsignedLong.valueOf(streamFrameTestVector.maxStreamId().get()))
            .build();
        break;
      }
      case ConnectionStreamIdBlocked: {
        frameFromTestVectorFile = ConnectionStreamIdBlockedFrame.builder()
            .maxStreamId(UnsignedLong.valueOf(streamFrameTestVector.maxStreamId().get()))
            .build();
        break;
      }
      case StreamClose: {
        frameFromTestVectorFile = StreamCloseFrame.builder()
            .streamId(UnsignedLong.valueOf(streamFrameTestVector.streamId().get()))
            .errorCode(ErrorCode.of(streamFrameTestVector.errorCode().get()))
            .errorMessage(streamFrameTestVector.errorMessage().get())
            .build();
        break;
      }
      case StreamMoney: {
        frameFromTestVectorFile = StreamMoneyFrame.builder()
            .streamId(UnsignedLong.valueOf(streamFrameTestVector.streamId().get()))
            .shares(UnsignedLong.valueOf(streamFrameTestVector.shares().get()))
            .build();
        break;
      }
      case StreamMoneyMax: {
        frameFromTestVectorFile = StreamMoneyMaxFrame.builder()
            .streamId(UnsignedLong.valueOf(streamFrameTestVector.streamId().get()))
            .totalReceived(UnsignedLong.valueOf(streamFrameTestVector.totalReceived().get()))
            .receiveMax(UnsignedLong.valueOf(streamFrameTestVector.receiveMax().get()))
            .build();
        break;
      }
      case StreamMoneyBlocked: {
        frameFromTestVectorFile = StreamMoneyBlockedFrame.builder()
            .streamId(UnsignedLong.valueOf(streamFrameTestVector.streamId().get()))
            .sendMax(UnsignedLong.valueOf(streamFrameTestVector.sendMax().get()))
            .totalSent(UnsignedLong.valueOf(streamFrameTestVector.totalSent().get()))
            .build();
        break;
      }
      case StreamData: {
        frameFromTestVectorFile = StreamDataFrame.builder()
            .streamId(UnsignedLong.valueOf(streamFrameTestVector.streamId().get()))
            .offset(UnsignedLong.valueOf(streamFrameTestVector.offset().get()))
            .data(Base64.getDecoder().decode(streamFrameTestVector.base64Data().get()))
            .build();
        break;
      }
      case StreamDataMax: {
        frameFromTestVectorFile = StreamDataMaxFrame.builder()
            .streamId(UnsignedLong.valueOf(streamFrameTestVector.streamId().get()))
            .maxOffset(UnsignedLong.valueOf(streamFrameTestVector.maxOffset().get()))
            .build();
        break;
      }
      case StreamDataBlocked: {
        frameFromTestVectorFile = StreamDataBlockedFrame.builder()
            .streamId(UnsignedLong.valueOf(streamFrameTestVector.streamId().get()))
            .maxOffset(UnsignedLong.valueOf(streamFrameTestVector.maxOffset().get()))
            .build();
        break;
      }
      case ConnectionAssetDetails: {
        frameFromTestVectorFile = ConnectionAssetDetailsFrame.builder()
            .sourceAssetCode(streamFrameTestVector.sourceAssetCode().get())
            .sourceAssetScale(streamFrameTestVector.sourceAssetScale().get())
            .build();
        break;
      }
    }

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    StreamCodecContextFactory.oer().write(frameFromTestVectorFile, byteArrayOutputStream);

    assertThat(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()))
        .isEqualTo(streamFrameTestVector.expectedAsn1OerBytes());

    // Read the bytes, and ensure they match.
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    final StreamFrame decodedStreamFrame = StreamCodecContextFactory.oer()
        .read(StreamFrame.class, byteArrayInputStream);
    assertThat(decodedStreamFrame).isEqualTo(frameFromTestVectorFile);
  }
}
