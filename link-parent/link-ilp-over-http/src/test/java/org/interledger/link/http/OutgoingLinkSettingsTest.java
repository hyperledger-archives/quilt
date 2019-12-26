package org.interledger.link.http;

import okhttp3.HttpUrl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OutgoingLinkSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void validateSimpleAuthSettingsRequired() {
    expectedException.expectMessage("simpleAuthSettings required");
    OutgoingLinkSettings.builder()
        .url(HttpUrl.parse("http://test.com"))
        .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
        .build();
  }

  @Test
  public void validateJwtSettingsRequiredForHS256() {
    expectedException.expectMessage("jwtAuthSettings required");
    OutgoingLinkSettings.builder()
        .url(HttpUrl.parse("http://test.com"))
        .authType(IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
        .build();
  }

  @Test
  public void validateJwtSettingsRequiredForRS256() {
    expectedException.expectMessage("jwtAuthSettings required");
    OutgoingLinkSettings.builder()
        .url(HttpUrl.parse("http://test.com"))
        .authType(IlpOverHttpLinkSettings.AuthType.JWT_RS_256)
        .build();
  }

}