/*-
 * ========================LICENSE_START=================================
 * Interledger Core Codecs
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
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

package org.interledger.codecs.stream.frame;

import org.interledger.stream.Denomination;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests for {@link AsnConnectionAssetDetailsFrameCodec}.
 */
@RunWith(Parameterized.class)
public class AsnConnectionAssetDetailsFrameCodecTest extends AbstractAsnFrameCodecTest<ConnectionAssetDetailsFrame> {

  public AsnConnectionAssetDetailsFrameCodecTest() {
    super(ConnectionAssetDetailsFrame.class);
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {

    return Arrays.asList(new Object[][] {
        // assetScale=0
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("USD")
                        .assetScale((short) 0)
                        .build()
                ).build()
        },
        // assetScale=1
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("USD")
                        .assetScale((short) 1)
                        .build()
                ).build()
        },
        // assetScale=10
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("USD")
                        .assetScale((short) 10)
                        .build()
                ).build()
        },
        // assetScale=Max-1
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("USD")
                        .assetScale((short) 254)
                        .build()
                ).build()
        },
        // assetScale=Max
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("USD")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },

        // empty code
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },
        // ShortCode=0
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("U")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },
        // MediumCode
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("US")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("USD")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },
        // LongCode
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("DASH")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },
        // LongerCode
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("DASH-DASH-DASH")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },
        // Chinese String (UTF)
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("元元元")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },
        // Russian String (UTF)
        {
            ConnectionAssetDetailsFrame.builder()
                .sourceDenomination(
                    Denomination.builder()
                        .assetCode("₽₽₽")
                        .assetScale((short) 255)
                        .build()
                ).build()
        },
    });
  }

}
