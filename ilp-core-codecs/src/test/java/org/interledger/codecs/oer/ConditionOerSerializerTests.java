package org.interledger.codecs.oer;

import org.interledger.codecs.InterledgerCodecContextFactory;
import org.interledger.codecs.framework.CodecContext;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
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
    return Arrays.asList(new Object[][]{{new PreimageSha256Condition(32, new byte[32])},
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

    final Condition decodedCondition = (Condition) context.read(Condition.class, byteArrayInputStream);
    MatcherAssert.assertThat(decodedCondition, CoreMatchers.is(condition));
  }
}
