package org.interledger.link.http;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IncomingLinkSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void validateSimpleAuthSettingsRequired() {
    expectedException.expectMessage("simpleAuthSettings required");
    IncomingLinkSettings.builder()
        .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
        .build();
  }

  @Test
  public void validateJwtSettingsRequiredForHS256() {
    expectedException.expectMessage("jwtAuthSettings required");
    IncomingLinkSettings.builder()
        .authType(IlpOverHttpLinkSettings.AuthType.JWT_HS_256)
        .build();
  }

  @Test
  public void validateJwtSettingsRequiredForRS256() {
    expectedException.expectMessage("jwtAuthSettings required");
    IncomingLinkSettings.builder()
        .authType(IlpOverHttpLinkSettings.AuthType.JWT_RS_256)
        .build();
  }

}