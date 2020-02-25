package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;

import okhttp3.HttpUrl;
import org.junit.Test;

public class MultilateralUrlFactoryTest {

  private static final InterledgerAddress OPERATOR = InterledgerAddress.of("g.operator");
  private static final HttpUrl MULTILATERAL_URL = HttpUrl.parse("https://www.test.com/spsp-%/ilp");
  private static final HttpUrl NON_MULTILATERAL_URL = HttpUrl.parse("https://www.test.com/spsp/ilp");

  @Test
  public void getOutgoingUrlWithMultilateralUrl() {
    MultilateralUrlFactory factory = new MultilateralUrlFactory(() -> OPERATOR, MULTILATERAL_URL);
    assertThat(factory.getOutgoingUrl(OPERATOR.with("spsp").with("0").with("secretstuff")))
        .isEqualTo(HttpUrl.parse("https://www.test.com/spsp-0/ilp"));
    assertThat(factory.getOutgoingUrl(OPERATOR.with("spsp").with("ONE").with("secretstuff")))
        .isEqualTo(HttpUrl.parse("https://www.test.com/spsp-ONE/ilp"));
  }

  @Test
  public void getOutgoingUrlWithNonMultilateral() {
    MultilateralUrlFactory factory = new MultilateralUrlFactory(() -> OPERATOR, NON_MULTILATERAL_URL);
    assertThat(factory.getOutgoingUrl(OPERATOR.with("spsp").with("0").with("secretstuff")))
        .isEqualTo(NON_MULTILATERAL_URL);
    assertThat(factory.getOutgoingUrl(OPERATOR.with("spsp").with("ONE").with("secretstuff")))
        .isEqualTo(NON_MULTILATERAL_URL);
  }

  @Test
  public void getOutgoingUrlWithNonMultilateralAddress() {
    MultilateralUrlFactory factory = new MultilateralUrlFactory(() -> OPERATOR, MULTILATERAL_URL);
    assertThat(factory.getOutgoingUrl(OPERATOR.with("spsp")))
        .isEqualTo(MULTILATERAL_URL);
  }



}