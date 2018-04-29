package org.interledger.core.asn.codecs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.core.Condition;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
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

@RunWith(Parameterized.class)
public class ConditionOerSerializerTests {

  // first data value (0) is default
  @Parameter
  public Condition condition;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays
        .asList(new Object[][] {{Condition.of(new byte[32])},
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

    final Condition decodedCondition = context.read(Condition.class, byteArrayInputStream);
    assertThat(decodedCondition, is(condition));
  }
}
