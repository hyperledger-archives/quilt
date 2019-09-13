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

import org.interledger.core.InterledgerAddress;
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
public class InterledgerAddressOerSerializerTests {

  private static final String JUST_RIGHT =
      "g.foo.0123"
          + "45678901234567890123456789012345678901234567890123456789012345678901234567890123456"
          + "78901234567890123456789012345678901234567890123456789012345678901234567890123456789"
          + "01234567890123456789012345678901234567890123456789012345678901234567890123456789012"
          + "34567890123456789012345678901234567890123456789012345678901234567890123456789012345"
          + "67890123456789012345678901234567890123456789012345678901234567890123456789012345678"
          + "90123456789012345678901234567890123456789012345678901234567890123456789012345678901"
          + "23456789012345678901234567890123456789012345678901234567890123456789012345678901234"
          + "56789012345678901234567890123456789012345678901234567890123456789012345678901234567"
          + "89012345678901234567890123456789012345678901234567890123456789012345678901234567890"
          + "12345678901234567890123456789012345678901234567890123456789012345678901234567890123"
          + "45678901234567890123456789012345678901234567890123456789012345678901234567890123456"
          + "78901234567890123456789012345678901234567890123456789012345678901234567890123456789"
          + "01234567891234567";

  // first data value (0) is default
  @Parameter
  public InterledgerAddress interledgerAddress;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {InterledgerAddress.builder().value("test1.foo").build()},
        {InterledgerAddress.builder().value("test3.foo.bar").build()},
        {InterledgerAddress.builder().value("test2.foo.bar").build()},
        {InterledgerAddress.builder().value("test1.foo.bar").build()},
        {InterledgerAddress.builder().value("test1.foo.bar.baz.bazz").build()},
        {InterledgerAddress.builder().value("private.secret.account").build()},
        {InterledgerAddress.builder().value("example.foo.bar").build()},
        {InterledgerAddress.builder().value("peer.foo.bar").build()},
        {InterledgerAddress.builder().value("self.foo.bar").build()},
        {InterledgerAddress.builder().value("g.foo").build()},
        {InterledgerAddress.builder().value(JUST_RIGHT).build()},
    });
  }

  @Test
  public void testEqualsHashcode() throws Exception {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(interledgerAddress, outputStream);

    final ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(outputStream.toByteArray());

    final InterledgerAddress decodedInterledgerAddress =
        context.read(InterledgerAddress.class, byteArrayInputStream);
    assertThat(decodedInterledgerAddress.getClass().getName()).isEqualTo(interledgerAddress.getClass().getName());
    assertThat(decodedInterledgerAddress).isEqualTo(interledgerAddress);
  }
}
