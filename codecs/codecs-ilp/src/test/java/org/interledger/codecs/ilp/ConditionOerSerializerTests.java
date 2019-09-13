package org.interledger.codecs.ilp;

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

import org.interledger.core.InterledgerCondition;
import org.interledger.encoding.asn.framework.CodecContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ConditionOerSerializerTests {

  // first data value (0) is default
  @Parameter
  public InterledgerCondition condition;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays
        .asList(new Object[][] {{InterledgerCondition.of(new byte[32])},
            // TODO: Some more test values
        });
  }

  @Test
  public void testEqualsHashcode() throws Exception {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(condition, outputStream);

    final ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(outputStream.toByteArray());

    final InterledgerCondition decodedCondition = context
        .read(InterledgerCondition.class, byteArrayInputStream);
    assertThat(decodedCondition).isEqualTo(condition);
  }
}
